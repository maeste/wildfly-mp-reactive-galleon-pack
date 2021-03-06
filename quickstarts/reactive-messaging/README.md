# WildFly Microprofile Reactive Messaging Quickstarts

## Prerequisites 
You will need to have the following installed on your machine:

* A Docker environment
* Galleon downloaded and available on your path as described in the [main README](/). The main README also provides
background on how to provision WildFly servers and which Galleon layers are available. 

## Structure
This consists of the following modules
* [core/](core) - This contains the common code for the application, and will be described in more detail below.
* 'Specialisations' - Each of these uses the code from the `core/` module, and provides a `docker-compose.yml` to install 
the target messaging system. They also provide a `provision.xml` to provision a WildFly server with the relevant 
Galleon layers installed. Finally they contain a 
`src/main/resources/META-INF/microprofile-config.properties` which configures the application for use with the target
messaging system while reusing the code. These are in the following sub-modules
    * [kafka/](kafka) - This uses Kafka as the messaging system
    * [amqp/](amqp) - This uses AMQP as the messaging system

## How to run it
First you need to build the contents of this repository:
```
mvn install -DskipTests
```
Then using a terminal you go into the folder of the relevant child module (e.g. `kafka/`), and start the messaging 
system by running:
```
docker-compose up
```
Next you need to provision a server (remember you need to have Galleon installed as described in 
the [main README](/)). You can do this by going to the relevant child module (e.g. `kafka/`) in a new
terminal, and then run:
```
galleon.sh provision ./provision.xml --dir=target/my-wildfly
./target/my-wildfly/bin/standalone.sh
```
This provisions the server with the relevant Galleon layers, and starts it.

Then in another terminal window, go to the relevant child module directory and run:
```
mvn package wildfly:deploy
```
This builds and deploys the application into the provisioned WildFly server.

Finally go to http://localhost:8080/quickstart/ and see the prices be updated from the application.

## Code structure

### Price Generator
The [PriceGenerator](core/src/main/java/org/wildfly/extras/quickstart/microprofile/reactive/messaging/PriceGenerator.java) 
is a class containing a method that generates some random prices every 5 seconds:
```
@ApplicationScoped
public class PriceGenerator {
    private Random random = new Random();

    @Outgoing("generated-price")
    public Flowable<Integer> generate() {
        return Flowable.interval(5, TimeUnit.SECONDS)
                .map(tick -> random.nextInt(100));
    }
}
```
It is an `ApplicationScoped` CDI bean. The important part is the `@Outgoing` annotation, which pushes the values
from the `Flowable` reactive stream returned by the method to the `generated-price` stream. Later we will see how
we use a `META-INF/microprofile-config.properties` to bind this stream to the underlying messaging provider.

### Price Converter
Next we have the [PriceConverter](core/src/main/java/org/wildfly/extras/quickstart/microprofile/reactive/messaging/PriceConverter.java)
which reads values off a stream and transforms them:
```
@ApplicationScoped
public class PriceConverter {
    private static final double CONVERSION_RATE = 0.88;

    @Incoming("prices")
    @Outgoing("my-data-stream")
    @Broadcast
    public double process(int priceInUsd) {
        return priceInUsd * CONVERSION_RATE;
    }
```
This method consumes values off the `prices` topic/stream, does some conversion, and the value returned from the method
is sent to the `my-data-stream` stream. The `@BroadCast` annotation is a SmallRye (the implementation we are using
in this feature pack to support Reactive Messaging) extension to the Reactive Messaging specification.
The `Broadcast` annotation allows more than one subscriber to a stream (normally there must be a one to one mapping) and
all subscribers will receive the value.
**Note:** `my-data-stream` is an in-memory stream which is not connected to a messaging provider. We will consume this
stream in the `PriceResource` in the next step.

### Price Resource
Finally we have a JAX-RS resource implemented in [PriceResource](core/src/main/java/org/wildfly/extras/quickstart/microprofile/reactive/messaging/PriceResource.java)
```
@Path("/prices")
public class PriceResource {
    @Inject
    @Channel("my-data-stream") Publisher<Double> prices;


    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS) // denotes that server side events (SSE) will be produced
    @SseElementType("text/plain") // denotes that the contained data, within this SSE, is just regular text/plain data
    public Publisher<Double> stream() {
        return prices;
    }
}
```
This does a few things:
* The `@Path` annotation binds the JAX-RS resource to the `/prices` path.
* We inject the `my-data-stream` channel using the `@Channel` qualifier. This allows us to take data from reactive 
messaging and push them to the 'imperative' part of our application. `@Channel` is a SmallRye extension to the
Reactive Messaging specification.
* The `stream()` method produces server side events of type `text/plain`, and returns the `prices` stream that was 
injected in the previous step.

### The HTML page
The [index.html](core/src/main/webapp/index.html) page consumes the server sent events and displays those:
```
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Prices</title>

    <link rel="stylesheet" type="text/css"
          href="https://cdnjs.cloudflare.com/ajax/libs/patternfly/3.24.0/css/patternfly.min.css">
    <link rel="stylesheet" type="text/css"
          href="https://cdnjs.cloudflare.com/ajax/libs/patternfly/3.24.0/css/patternfly-additions.min.css">
</head>
<body>
<div class="container">

    <h2>Last price</h2>
    <div class="row">
    <p class="col-md-12">The last price is <strong><span id="content">N/A</span>&nbsp;&euro;</strong>.</p>
    </div>
</div>
</body>
<script src="https://code.jquery.com/jquery-3.3.1.min.js"></script>
<script>
    var source = new EventSource("/prices/stream");
    source.onmessage = function (event) {
        document.getElementById("content").innerHTML = event.data;
    };
</script>
</html>
```
As prices are published in the JAX-RS resource, they are displayed on the page.

### Mapping the streams
See each of the child modules for how we map our application's streams to the underlying messaging provider:
* [amqp/](amqp/)
* [kafka/](kafka/)


## Further reading
* [MicroProfile Reactive Messaging specification](https://github.com/eclipse/microprofile-reactive-messaging/releases)
* [SmallRye Reactive Messaging documentation](https://smallrye.io/smallrye-reactive-messaging/)
* [SmallRye Reactive Messaging Source](https://github.com/smallrye/smallrye-reactive-messaging)  