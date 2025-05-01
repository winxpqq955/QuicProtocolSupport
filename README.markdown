## Usage

Prepend `quic://` to the Minecraft server address. (`tcp://` can also be used, but it has no effect as TCP is the default.)

## Credits

Significant portions of code have been reworked from [quic-connect](https://github.com/ramidzkh/quic-connect)
by [ramidzkh (Ramid Khan)](https://github.com/ramidzkh).

## Footnotes

- You must use a valid, CA-signed TLS certificate on the server.
	- If you want to use a self-signed certificate, please compile the mod with `.trustManager(io.netty.handler.ssl.util.InsecureTrustManagerFactory.INSTANCE)` uncommented.
- Only these platforms are supported:
	- Windows:
		- `x86-64`
	- Linux:
		- `x86-64`
		- `ARM64`
