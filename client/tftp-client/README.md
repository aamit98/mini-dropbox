# TFTP Client

This project implements a TFTP (Trivial File Transfer Protocol) client in Java. The client allows users to upload and download files to and from a TFTP server, manage socket connections, and process user commands.

## Project Structure

```
tftp-client
├── client
│   └── src
│       └── main
│           └── java
│               └── bgu
│                   └── spl
│                       └── net
│                           └── impl
│                               └── tftp
│                                   └── TftpClient.java
├── pom.xml
└── README.md
```

## Requirements

- Java Development Kit (JDK) 8 or higher
- Maven 3.6 or higher

## Building the Project

To build the project, navigate to the root directory of the project and run the following command:

```
mvn clean install
```

This command will compile the Java source files and package the application.

## Running the TFTP Client

After building the project, you can run the TFTP client using the following command:

```
java -cp target/tftp-client-1.0-SNAPSHOT.jar bgu.spl.net.impl.tftp.TftpClient [host] [port]
```

Replace `[host]` with the TFTP server's address (default is `127.0.0.1`) and `[port]` with the TFTP server's port (default is `7777`).

## Usage

Once the client is running, you can use the following commands:

- `RRQ <filename>`: Request to download a file from the server.
- `WRQ <filename>`: Request to upload a file to the server.
- `DIRQ`: Request to list files in the current directory on the server.
- `DELRQ <filename>`: Request to delete a file on the server.
- `LOGRQ <filename>`: Request to log in to the server.
- `DISC`: Disconnect from the server.

## License

This project is licensed under the MIT License. See the LICENSE file for more details.