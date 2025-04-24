# <sub><img src="src/main/resources/assets/icon.png" alt="" width="40"/></sub> QuicProtocolSupport
## Description
Adds support for Minecraft over QUIC

## Usage
Prepend `quic://` to the Minecraft server address.

## Footnotes
- You must use a valid, CA-signed TLS certificate on the server.
	- If you want to use a self-signed certificate, please compile the client with the
	  `.trustManager(io.netty.handler.ssl.util.InsecureTrustManagerFactory.INSTANCE)` line uncommented.

## Credits
Significant portions of code have been reworked from [quic-connect](https://github.com/ramidzkh/quic-connect)
by [ramidzkh (Ramid Khan)](https://github.com/ramidzkh).
