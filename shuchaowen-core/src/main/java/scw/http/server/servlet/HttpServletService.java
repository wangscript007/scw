package scw.http.server.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface HttpServletService {
	void service(HttpServletRequest request, HttpServletResponse response) throws IOException;
}