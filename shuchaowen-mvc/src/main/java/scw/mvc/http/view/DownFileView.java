package scw.mvc.http.view;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import scw.io.IOUtils;
import scw.mvc.http.HttpChannel;
import scw.mvc.http.HttpRequest;
import scw.mvc.http.HttpResponse;
import scw.mvc.http.HttpView;

public final class DownFileView extends HttpView {
	private String encoding;
	private int buffSize = 1024 * 8;
	private File file;

	public DownFileView(File file) {
		this.file = file;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public void setBuffSize(int buffSize) {
		this.buffSize = buffSize;
	}

	@Override
	public void render(HttpChannel channel, HttpRequest httpRequest, HttpResponse httpResponse) throws Throwable {
		if (encoding != null) {
			httpResponse.setCharacterEncoding(encoding);
		}

		httpResponse.setContentType(Files.probeContentType(file.toPath()));
		httpResponse.setHeader("Content-Disposition",
				"attachment;filename=" + new String(file.getName().getBytes(), "IOS-8859-1"));
		httpResponse.setBufferSize(buffSize);

		FileInputStream fis = null;
		OutputStream os = null;
		try {
			os = httpResponse.getOutputStream();
			fis = new FileInputStream(file);
			IOUtils.write(fis, os, buffSize);
		} finally {
			IOUtils.close(fis, os);
		}
	}
}