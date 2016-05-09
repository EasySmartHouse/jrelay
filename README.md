# jrelay [![Build Status](https://travis-ci.org/EasySmartHouse/jrelay.svg)](https://travis-ci.org/EasySmartHouse/jrelay)##
This library allows you to use your relay connected to computer directly from Java. It's designed to abstract commonly used relay's drivers and has very simple API.

## Rationale
Today we have a huge amount of various relay on the market with cardinal differences in hardware and driver part, that often used as a switch for the home devices. This way we may use relay to control almost every device in our home and jrelay API was created to remove the burden of situations where you have to rewrite your code because of relay replacing, but instead you can simply switch the driver class to different one.

## Features
* Simple, thread-safe and non-blocking API,
* No additional software required,
* Supports multiple platforms (Windows, Linux, Mac OS, etc) and various architectures,
* It is available as Maven dependency or standalone ZIP binary (with all dependencies included),
* Multiple relay drivers are supported.

## Supported devices
![RS232 relay](http://s32.postimg.org/8xwiwr9dt/RS232_relay_1.jpg "RS232 relay")
![RS232 relay](http://s32.postimg.org/puiugxnb5/RS232_relay_2.jpg "RS232 relay")
![USB relay](http://s32.postimg.org/9xycwcsht/USB_relay_1.jpg "USB relay")
![USB relay](http://s32.postimg.org/7agprczep/USB_relay_2.jpg "USB relay")
![USB relay](http://s32.postimg.org/vf43n7wht/USB_relay_3.jpg "USB relay")

## Maven

The latest stable version is available in EasySmartHouse github repo:

1. Add repository:
```xml
<repository>
	<id>jrelay-mvn-repo</id>
    <url>https://raw.github.com/EasySmartHouse/jrelay/mvn-repo/</url>
    <snapshots>
       <enabled>true</enabled>
       <updatePolicy>always</updatePolicy>
    </snapshots>
</repository>
```

2. Add core dependency:
```xml
<dependency>
	<groupId>com.github.jrelay</groupId>
	<artifactId>jrelay-core</artifactId>
	<version>0.1-SNAPSHOT</version>
</dependency>
```

3. Add desired driver:
```xml
<dependency>
	<groupId>com.github.jrelay</groupId>
	<artifactId>driver-usbhid</artifactId>
	<version>0.1-SNAPSHOT</version>
</dependency>

<dependency>
	<groupId>com.github.jrelay</groupId>
	<artifactId>driver-jssc</artifactId>
	<version>0.1-SNAPSHOT</version>
</dependency>
```

## Download

Click on  to download it:

 [jrelay-0.1-repo.zip](https://github.com/EasySmartHouse/jrelay/archive/0.1-repo.zip)

## Hello World

Code below will just open and then close a relay after 1 second:

```java
public class HelloWorld {

    static {
        // set all available drivers
        Relay.setDrivers(UsbHidRelayDriver.class, JsscRelayDriver.class);
    }

    public static void main(String[] args) throws Exception{
        //get all available relays
        Relay relay = Relay.getDefault();
        //open relay
        relay.open();
        //wait 1 second
        Thread.sleep(1000l);
        //relay close
        relay.close();
    }
}
```
This example is available [here](https://github.com/EasySmartHouse/jrelay-hello-world)
