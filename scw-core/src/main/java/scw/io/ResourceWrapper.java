package scw.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;

import scw.io.event.ResourceEventDispatcher;

public abstract class ResourceWrapper implements Resource {

	public abstract Resource getResource();

	public InputStream getInputStream() throws IOException {
		return getResource().getInputStream();
	}

	public OutputStream getOutputStream() throws IOException {
		return getResource().getOutputStream();
	}

	public boolean exists() {
		return getResource().exists();
	}

	public boolean isReadable() {
		return getResource().isReadable();
	}

	public boolean isWritable() {
		return getResource().isWritable();
	}

	public boolean isOpen() {
		return getResource().isOpen();
	}

	public URL getURL() throws IOException {
		return getResource().getURL();
	}

	public URI getURI() throws IOException {
		return getResource().getURI();
	}

	public File getFile() throws IOException, FileNotFoundException {
		return getResource().getFile();
	}

	public long contentLength() throws IOException {
		return getResource().contentLength();
	}

	public long lastModified() throws IOException {
		return getResource().lastModified();
	}

	public Resource createRelative(String relativePath) throws IOException {
		return getResource().createRelative(relativePath);
	}

	public String getFilename() {
		return getResource().getFilename();
	}

	public String getDescription() {
		return getResource().getDescription();
	}

	public boolean isSupportEventDispatcher() {
		return getResource().isSupportEventDispatcher();
	}

	public ResourceEventDispatcher getEventDispatcher() {
		return getResource().getEventDispatcher();
	}
	
	@Override
	public String toString() {
		return getResource().toString();
	}
}
