# [Yoke](/)

## Benchmarks

Yoke has a simple benchmark to compare to ExpressJS, these benchmarks are too simple and do not represent real world
scenarios. Proper benchmarking should be done comparing what you need and not just serving static text or json
resources.

## ExpressJS code

``` javascript
var express = require('express');
var app = express();

app.configure(function(){
    app.use(express.bodyParser());
});

app.get('/', function(req, res){
    res.send('Hello World\n');
});

app.get('/json', function(req, res){
    res.send({ name: 'Tobi', role: 'admin' });
});

function foo(req, res, next) {
    next();
}

app.get('/middleware', foo, foo, foo, foo, function(req, res){
    res.send('1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890');
});

app.listen(8000);
```

This application was running node *v0.10.3* and express *3.2.4* and in a single thread.


## Yoke code

``` java
public class YokeBench extends Verticle {

    @Override
    public void start() {

        final Middleware foo = new Middleware() {
            @Override
            public void handle(YokeRequest request, Handler<Object> next) {
                next.handle(null);
            }
        };

        new Yoke(vertx)
                .use(new BodyParser())
                .use("/middleware", foo)
                .use("/middleware", foo)
                .use("/middleware", foo)
                .use("/middleware", foo)
                .use(new Router()
                        .get("/", new Handler<YokeRequest>() {
                            @Override
                            public void handle(YokeRequest request) {
                                request.response().end("Hello World\n");
                            }
                        })
                        .get("/json", new Handler<YokeRequest>() {
                            @Override
                            public void handle(YokeRequest request) {
                                request.response().end(new JsonObject().putString("name", "Tobi").putString("role", "admin"));
                            }
                        })
                        .get("/middleware", new Handler<YokeRequest>() {
                            @Override
                            public void handle(YokeRequest request) {
                                request.response().end("1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890");
                            }
                        })
                ).listen(8080);
    }
}
```

This application was running java *1.7.0_21 (Oracle)*, yoke *1.0.0-SNAPSHOT* and vert.x *2.0.0-SNAPSHOT* and in a single
thread.


## Cluster

Although **both** frameworks run in a single Thread, in a real production environment you might want to use all your
cores to get all the performance out of the box.


## ExpressJS code

``` javascript
// Include the cluster module
var cluster = require('cluster');

// Code to run if we're in the master process
if (cluster.isMaster) {

    // Count the machine's CPUs
    var cpuCount = require('os').cpus().length;

    // Create a worker for each CPU
    for (var i = 0; i < cpuCount; i += 1) {
        cluster.fork();
    }

    // Listen for dying workers
    cluster.on('exit', function (worker) {

        // Replace the dead worker, we're not sentimental
        console.log('Worker ' + worker.id + ' died :(');
        cluster.fork();

    });

// Code to run if we're in a worker process
} else {

    // Include Express
    var express = require('express');

    // Create a new Express application
    var app = express();

    // Add a basic route – index page
    app.get('/', function (req, res) {
        res.send('Hello World\n');
    });

    // Bind to a port
    app.listen(3000);
    console.log('Worker ' + cluster.worker.id + ' running!');

}
```

Lots of code here!

## Yoke code

``` java
public class YokeBench extends Verticle {

    @Override
    public void start() {

        new Yoke(vertx)
                .use(new Router()
                        .get("/", new Handler<YokeRequest>() {
                            @Override
                            public void handle(YokeRequest request) {
                                request.response().end("Hello World\n");
                            }
                        })
                ).listen(8080);
    }
}
```

As you can read Yoke code is cleaner and it is not the concern of the developer to implement forking as that is managed
by the underlying Vert.x framework.


## Results

### Serving text

![Hello World expressJS vs Yoke](text.png)

As it can be seem Yoke is faster and scales the same way ExpressJS does.

### Serving json

![JSON expressJS vs Yoke](json.png)

As it can be seem Yoke is faster and scales the same way ExpressJS does.

### Middleware

![Middleware expressJS vs Yoke](middleware.png)

As it can be seem Yoke is faster and scales the same way ExpressJS does.

## Cluster Results

![Cluster expressJS vs Yoke](cluster.png)

Running the benchmark using 4 workers (the test machine has 4 cores) both with Express and Yoke you can see that Yoke
is a clear winner.

## Conclusions

These benchmarks are quite naive but give you a good impression that if you have a fast expressJS application and you
port it to Yoke you will get some more performance and the expected scalability.