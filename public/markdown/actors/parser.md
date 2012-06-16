# Actors

The simplest actor possible needs only a receive method:

    class SimpleActor extends Actor {
      def receive = {
        case _ =>
      }
    }

The `receive` method will be invoked when
another actor sends sends the object a message.

Generally you should never have a direct reference to another actor.
Messages are sent to actors via indirect references.

## Play Framework

Actors must exist in a heirarchy, or actor system.
The play framework comes with a built-in
actor system available at `play.libs.Akka.system`.
The actor system creates and manages the actor references.

    import play.libs.Akka.system

    object Setup {
        val actorReference = system.actorOf(Props[SimpleActor])
    }

The actor can now be sent messages
by using the actor reference available at `Setup.actorReference`.

    Setup.actorReference ! "This is a string message"

## Asynchronous Messaging

Messages are always asynchronous;
sending a message returns immediately.

Generally actors communicate back and forth to each other.
For example, an actor in charge of a database
might serialize and deserialize objects for others.

### Replying

Actors can reply to each other by using `sender`,
an automatic refernece to the sending actor.

### Waiting for Replies

There are two ways to accept replies, stateful and stateless.

#### Stateless

Stateless replying is the _most_ asynchronous,
no special state is created by the sending actor.
The sending actor must know how to differentiate responses
from other messages in its `receive` method.

#### Stateful

A stateful response uses `ask` to ...

    actorRef ? message

The above returns a `Promise[Any]` monad,
thus we can use `map` and `flatmap` to compose actor responses.

The method `Promise.map` accepts a function of type `Any => Any`.

The outputs and inputs of a series of actors
can be plugged together using for-loop comprehension.

    for( reply1 <- actor1 ? message1;
         reply2 <- actor2 ? reply1 ) yield { reply2 }

The response from `actor1` (via `sender ! reponse`) will be piped directly to the `receive` method of `actor2`.



