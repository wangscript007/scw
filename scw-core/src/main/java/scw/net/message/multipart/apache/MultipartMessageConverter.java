package scw.net.message.multipart.apache;

import java.io.IOException;
import java.util.List;

import scw.core.ResolvableType;
import scw.http.DefaultHttpInputMessage;
import scw.http.HttpInputMessage;
import scw.net.MimeTypeUtils;
import scw.net.message.InputMessage;
import scw.net.message.converter.MessageConvertException;
import scw.net.message.multipart.FileItem;
import scw.net.message.multipart.FileItemParser;
import scw.net.message.multipart.MultipartMessageWriter;

public class MultipartMessageConverter extends MultipartMessageWriter {

	private final FileItemParser fileItemParser;

	public MultipartMessageConverter(FileItemParser fileItemParser) {
		this.fileItemParser = fileItemParser;
		supportMimeTypes.add(MimeTypeUtils.MULTIPART_FORM_DATA);
	}

	public FileItemParser getFileItemParser() {
		return fileItemParser;
	}

	@Override
	public boolean canRead(ResolvableType type) {
		if (getFileItemParser() == null) {
			return false;
		}

		if (Iterable.class.isAssignableFrom(type.getRawClass())) {
			return type.getGeneric(0).getRawClass() == FileItem.class;
		} else if (type.isArray()) {
			return type.getComponentType().getRawClass() == FileItem.class;
		}
		return false;
	}

	@Override
	protected Object readInternal(ResolvableType type, InputMessage inputMessage) throws IOException, MessageConvertException {
		List<FileItem> fileItems = getFileItemParser().parse(inputMessage instanceof HttpInputMessage
				? (HttpInputMessage) inputMessage : new DefaultHttpInputMessage(inputMessage));
		if (type.isArray()) {
			return fileItems.toArray(new FileItem[0]);
		}
		return fileItems;
	}
}
