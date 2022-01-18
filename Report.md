# Software and frameworks

I've used Leshan (https://github.com/eclipse/leshan) for the LWM2M server implementation. I've used the leshan-server-demo in particular, which already had implemented a web app for connecting the clients. I've altered the Vue `/webapp` folder inside this repository to make the webserver and the parking lot interface. 

The webserver is build into the LWM2M server. It connects via an API to the webinterface and the LwM2M clients are accessed through the CoAP protocol. Every time an update is fired the `api/event` endpoint is triggered which notifies the Vue interface. This results in an instant refresh of details when things change in the LWM2M server. 

I've added some API endpoints for the webinterface, including `api/parkingspots` to get a list of parkingspots and vehiclecounters. Also I've included `api/parkingspots/info` to retrieve a list of details, like the pay rate, available spots etc. Also, when we do a `PUT` request towards the `api/parkingspots` we can edit these details. For altering the LWM2M objects, I've used the already existing `api/clients/*` endpoint, which made it possible to alter every resource of a client individually.

At every response the server saves the information in a MySQL database. I run XAMPP, which has a build in MySQL server which the LWM2M server connects to. This might be something to improve on for in the future. Now only one table is used to dump all data in. Though, for now this is enough to do analysis on statistics of the parking lot. To see the MySQL working, you need to install a MySQL server and create the database `iotparking` which has the following table:

```sql
CREATE TABLE `stats` (
  `id` int(11) NOT NULL,
  `object` varchar(255) NOT NULL,
  `value` text NOT NULL,
  `datetime` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

I have also implemented the API located at `https://opendata.rdw.nl/resource/d3ku-w8e3.json?kenteken=xxx` to check whether a license plate is from a electric car or not. It does this check when 'Direction' is 1 and the license plate is filled in at the vehicle counter. It outputs info in the server terminal, whether the car is electric or not.

During testing, I've used `yarn serve` to run the webserver and my InteliJ IDE to run the LWM2M server using the class `LeshanServerDemo`. 

# How to run

I've made a build so you can run the webserver and the LWM2M server simultaneously using`java -jar target/leshan-server-demo-2.0.0-SNAPSHOT-jar-with-dependencies.jar` in the project folder. Once running, you can go to http://localhost:8080/#/parkinglot to see the parking lot interface.

For further details, my repo can be found here: https://github.com/Tomgroot/IoT-parking-system.

# Technical difficulties

I had difficulties with deciding which structure the project needed to have. Especially, because the project has three elements that needed to stay connected with each other (client, server and the web interface). I first tried creating a stand alone webserver in Java using the `com.sun.net.httpserver.HttpServer` package. Though, I could not find an easy way to update the elements on the webpage. It was either a static page or a page that couldn't be styled easily. 

I was already using the leshan-server-demo for the LWM2M server and noticed that it already included a Vue webinterface. This made me chose to go with Vue, as implementing the parkinglot was using the already existing webinterface, and altering this to my needs. It was the first time I used Vue so this was quiet new for me. 

# Work division

My group member has not worked on this project. I had to do this project by myself. I've mailed the professor about the fact that my teammate didn't respond to my messages. The deadline for this project was 17-1 and I asked him if I could hand in the assignment later without getting a point deduction. He mailed me that this was not a problem (see forwarded mail). 