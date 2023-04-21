package lv.lumii.httpws;

import java.io.BufferedReader;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpRequest;

public class HttpRequestFactory {

    // the user has to append the uri
    public static HttpRequest.Builder createRequestBuilder(Reader httpInput) throws Exception {
        BufferedReader reader;
        if (httpInput instanceof BufferedReader)
            reader = (BufferedReader) httpInput;
        else
            reader = new BufferedReader(httpInput);

        HttpRequest.Builder builder = HttpRequest.newBuilder();
        String[] requestLine = reader.readLine().split(" ");

        String header = reader.readLine();
        while (header.length() > 0) {
            int i = header.indexOf(": ");
            if (i>=0) {
                builder = builder.header(header.substring(0, i), header.substring(i+2));
            }
            header = reader.readLine();
        }

        StringBuffer body = new StringBuffer();
        String bodyLine = reader.readLine();
        while (bodyLine != null) {
            body.append(bodyLine+"\r\n");
            bodyLine = reader.readLine();
        }

        if ("GET".equals(requestLine[0]))
            builder = builder.GET();
        else if ("PUT".equals(requestLine[0]))
            builder = builder.PUT(HttpRequest.BodyPublishers.ofString(body.toString()));
        else if ("POST".equals(requestLine[0]))
            builder = builder.POST(HttpRequest.BodyPublishers.ofString(body.toString()));
        else if ("DELETE".equals(requestLine[0]))
            builder = builder.DELETE();
        else if ("HEAD".equals(requestLine[0]))
            builder = builder.HEAD();

        return builder;

    }

    public static HttpRequest createRequest(URI targetUri, Reader httpInput) throws Exception {
        HttpRequest.Builder builder = HttpRequestFactory.createRequestBuilder(httpInput);
        return builder.uri(targetUri).build();
    }

}