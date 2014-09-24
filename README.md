
                     ...88888888888888888888888888888888888...
                   ... 08                                  80...
                  ++..08   MMMMMMMMMMMMMMMMMMMMMMMMMMMMM    80..++
           OOOOOOOO..08   MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM   80..OOOOOOOO
          O ,..      08  MMMMMMM  MMMMM  MMMM  MMMM  MMMMMM  80          O
         O ,    `   08  MMMMMMM    MMM    MM    MM    MMMMMM  80   ,'''`  O
        O ,  ++  `  08  MMMMMMMM  MMMMM  MMMM  MMMM  MMMMMMM  80  '  ++ `  O
        O , ++++ ' 08  MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM  80 ' ++++ ' O
        O  . ++  ' 08  MMMMMM  MMMMM  MMMM  MMMM  MMMM  MMMMM  80  . ++  , O
         O  ....' 08  MMMMMM    MMM    MM    MM    MM    MMMMM  80  ....' O
          O       08  MMMMMMM  MMMMM  MMMM  MMMM  MMMM  MMMMMM  80       O
           OOOOO..8    MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM    8..OOOOO
               +.08     MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM     80.+
                  08                                            80
                    @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@


# serial-port

A simple library for serial port communication with Clojure. Although serial communciation may be considered old tech, it's useful for a communicating with a plethora of devices including exciting new hardware such as the [Monome](http://monome.org) and the [Arduino](http://arduino.cc).

## Dependencies

`serial-port` has the following dependencies:

* [Clojure 1.2 or 1.3](http://clojure.org)
* [RxTx](http://rxtx.qbang.org) (jar and native binary for your OS and CPU architecture)

## Installation

The easiest way to to install `serial-port` is requiring the [serial-port clojar](http://clojars.org/serial-port) in your `project.clj` and using [cake](http://clojure-cake.org/) to pull the dependencies. Cake has great support for native dependencies and the [serial-port clojar](http://clojars.org/serial-port) depends on the [rxtx22 clojar](http://clojars.org/rxtx22) which packages the [RxTx native libraries](http://rxtx.qbang.org).

All this means you simply need to add `serial-port` to your list of dependencies in your `project.clj`:

    (defproject your-project "0.1.5"
      :description "Your fabulous project that uses a serial connection"
      :dependencies [[org.clojure/clojure "1.3.0"]
                     [serial-port "1.1.0"]])

(Where `1.1.0` is replaced with the version you wish to use.)

Then run `cake deps` and cake will pull the correct dependencies (both native libs and jars) and put them in the right place for you to use with `cake repl` or `cake swank`.

## Usage

### Using the library

Just make sure you pull in the `serial-port` namespace using something like:

    (use 'serial-port)

### Finding your port identifier

In order to connect to your serial device you need to know the path of the file it presents itself on. `serial-port` provides a simple function to list these paths out:

    => (list-ports)

    0 : /dev/tty.usbmodemfa141
    1 : /dev/cu.usbmodemfa141
    2 : /dev/tty.Bluetooth-PDA-Sync
    3 : /dev/cu.Bluetooth-PDA-Sync
    4 : /dev/tty.Bluetooth-Modem
    5 : /dev/cu.Bluetooth-Modem

In this case, we have an Arduino connected to `/dev/tty.usbmodemfa141`.

#### Problem on Ubuntu Linux

From ubuntu linux 11.04 and forward, you may not see your device.
The RXTX library searches for serial ports with the `/dev/ttySxx` naming convention.
Your device may be called `/dev/ttyACM0` and so is not found.

From the repl point of view, you could make your system aware of it:

    (System/setProperty "gnu.io.rxtx.SerialPorts" "/dev/ttyACM0")

From the OS point of view:
You could create a symbolic link with a name that satisfies this, so for example:

    sudo ln -s /dev/ttyACM0 /dev/ttyS42

src: http://rxtx.qbang.org/wiki/index.php/Discovering_comm_ports

### Connecting with a port identifier

When you know the path to the serial port, connecting is just as simple as:

    (open "/dev/tty.usbmodemfa141")

However, you'll want to bind the result so you can use it later:

    (def port (open "/dev/tty.usbmodemfa141"))

### Reading bytes

The simplest way to read bytes from the connection is to use `on-byte`. This allows you to register a handler fn which will be called for each byte received. So, to print out each byte just do the following:

    (on-byte port #(println %))

It's also possible to register a handler for every n bytes. The monome communicates by sending pairs of bytes, one byte to describe whether a button was pressed or released, and another to describe the coordinates of the button. You can register a handler to receive pairs of bytes as follows:

    (on-n-bytes port 2 (fn [[action coords]] ...))

If you wish to get raw access to the `InputStream` this is possible with the function `listen`. This allows you to specify a handler that will get called every time there is data available on the port and will pass your handler the `InputStream` to allow you to directly `.read` bytes from it. Both `on-byte` and `on-n-bytes` generate such handlers acting as a proxy between your specified handler and the incoming data events.

When the handler is first registered, the bytes that have been buffered on the serial port are dropped by default. This can be changed by passing false to `on-byte`, `on-n-bytes` or `listen` as an optional last argument.

Only one listener may be registered at a time. If you want to fork the incoming datastream to a series of streams, you might want to consider using lamina. You can then register a handler which simply enqueues the incoming serial data to a lamina channel which you may then fork and map according to your whim.

Finally, you may remove your listener with `remove-listener`.

### Writing bytes

The simplest way to write bytes is by passing a byte array to `write`:

    (write port my-byte-array)

There are a couple of convenience functions available if you're dealing with plain Integers. `write-int` allows you to write a simple integer to the serial port and `write-int-seq` allows you to pass a sequence of integers which are then converted to a byte array which is subsequently written to the serial port:

    (write-int port 20)
    (write-int-seq port [20 10 2 100])

### Closing the port

Simply use the `close` function:

    (close port)

## Contributors

* Sam Aaron
* Jeff Rose

## License

Copyright (C) 2011 Sam Aaron

Distributed under the Eclipse Public License, the same as Clojure.
