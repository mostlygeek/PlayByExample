# Using Markdown in Play 2.0

Well using Markdown in Play 2.0 is not a common need for most people. 
It was included in Play By Example since we're writing a lot of text and it is a
far easier to mark up content in Markdown than hand writing HTML. 

Fortunately creating the Markdown controller provides good examples on: 

* Locating and loading data from disk
* Delegating work through an `Async` block to prevent blocking the handling of additional requests
* Installing the [markwrap](http://software.clapper.org/markwrap/) dependency 

## Locating and Loading Data from Disk

* the markdown files exist within the Play framework's directories
* we can locate these using Application.getFile()

## Using an `Async` block 

(todo)

## Installing Dependencies

(todo)
