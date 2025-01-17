package io.mosip.datashare.util;
import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.gson.Gson;

import io.mosip.datashare.constant.ApiName;
import io.mosip.datashare.dto.Metadata;
import io.mosip.datashare.dto.PasswordRequest;
import io.mosip.datashare.dto.SecretKeyRequest;
import io.mosip.datashare.dto.TokenRequestDTO;
import io.mosip.datashare.exception.ApiNotAccessibleException;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.core.util.TokenHandlerUtil;


/**
 * @author Sowmya The Class RestUtil.
 */
@Component
public class RestUtil {

	@Value("${data.share.default.resttemplate.httpclient.connections.max.per.host:20}")
	private int maxConnectionPerRoute;

	@Value("${data.share.default.resttemplate.httpclient.connections.max:100}")
	private int totalMaxConnection;

	private RestTemplate restTemplate;

	/** The environment. */
    @Autowired
    private Environment environment;

	/** The Constant AUTHORIZATION. */
    private static final String AUTHORIZATION = "Authorization=";
	private RestTemplate localRestTemplate;

	@PostConstruct
	private void loadRestTemplate() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		localRestTemplate = getRestTemplate();
	}

	/**
	 * Post api.
	 *
	 * @param                 <T> the generic type
	 * @param apiName         the api name
	 * @param pathsegments    the pathsegments
	 * @param queryParamName  the query param name
	 * @param queryParamValue the query param value
	 * @param mediaType       the media type
	 * @param requestType     the request type
	 * @param responseClass   the response class
	 * @return the t
	 * @throws ApiNotAccessibleException the api not accessible exception
	 */
	@SuppressWarnings("unchecked")
	public <T> T postApi(ApiName apiName, List<String> pathsegments, String queryParamName, String queryParamValue,
			MediaType mediaType, Object requestType, Class<?> responseClass) throws ApiNotAccessibleException {
		T result = null;
		String apiHostIpPort = environment.getProperty(apiName.name());
		UriComponentsBuilder builder = null;
		if (apiHostIpPort != null)
			builder = UriComponentsBuilder.fromUriString(apiHostIpPort);
		if (builder != null) {

			if (!((pathsegments == null) || (pathsegments.isEmpty()))) {
				for (String segment : pathsegments) {
					if (!((segment == null) || (("").equals(segment)))) {
						builder.pathSegment(segment);
					}
				}

			}
			if (!((queryParamName == null) || (("").equals(queryParamName)))) {
				String[] queryParamNameArr = queryParamName.split(",");
				String[] queryParamValueArr = queryParamValue.split(",");

				for (int i = 0; i < queryParamNameArr.length; i++) {
					builder.queryParam(queryParamNameArr[i], queryParamValueArr[i]);
				}
			}

        try {
            localRestTemplate = getRestTemplate();
				result = (T) localRestTemplate.postForObject(builder.toUriString(), setRequestHeader(requestType, mediaType),
						responseClass);

        } catch (Exception e) {
            throw new ApiNotAccessibleException(e);
			}
		}
        return result;
    }

	/**
	 * Gets the api.
	 *
	 * @param                 <T> the generic type
	 * @param apiName         the api name
	 * @param pathsegments    the pathsegments
	 * @param queryParamName  the query param name
	 * @param queryParamValue the query param value
	 * @param responseType    the response type
	 * @return the api
	 * @throws ApiNotAccessibleException the api not accessible exception
	 */
	@SuppressWarnings("unchecked")
	public <T> T getApi(ApiName apiName, List<String> pathsegments, String queryParamName, String queryParamValue,
			Class<?> responseType) throws ApiNotAccessibleException {

		String apiHostIpPort = environment.getProperty(apiName.name());
		T result = null;
		UriComponentsBuilder builder = null;
		UriComponents uriComponents = null;
		if (apiHostIpPort != null) {

			builder = UriComponentsBuilder.fromUriString(apiHostIpPort);
			if (!((pathsegments == null) || (pathsegments.isEmpty()))) {
				for (String segment : pathsegments) {
					if (!((segment == null) || (("").equals(segment)))) {
						builder.pathSegment(segment);
					}
				}

			}

			if (!((queryParamName == null) || (("").equals(queryParamName)))) {

				String[] queryParamNameArr = queryParamName.split(",");
				String[] queryParamValueArr = queryParamValue.split(",");
				for (int i = 0; i < queryParamNameArr.length; i++) {
					builder.queryParam(queryParamNameArr[i], queryParamValueArr[i]);
				}

			}
			uriComponents = builder.build(false).encode();

        try {
            localRestTemplate = getRestTemplate();
				result = (T) localRestTemplate
						.exchange(uriComponents.toUri(), HttpMethod.GET, setRequestHeader(null, null), responseType)
                    .getBody();
        } catch (Exception e) {
            throw new ApiNotAccessibleException(e);
        }

		}
		return result;
    }

	@SuppressWarnings("unchecked")
	public <T> T getApi(ApiName apiName, Map<String, String> pathsegments, Class<?> responseType) throws Exception {

		String apiHostIpPort = environment.getProperty(apiName.name());
		T result = null;
		UriComponentsBuilder builder = null;
		UriComponents uriComponents = null;
		if (apiHostIpPort != null) {

			builder = UriComponentsBuilder.fromUriString(apiHostIpPort);

			URI urlWithPath = builder.build(pathsegments);

			try {
				localRestTemplate = getRestTemplate();
				result = (T) localRestTemplate
						.exchange(urlWithPath, HttpMethod.GET, setRequestHeader(null, null), responseType).getBody();
			} catch (Exception e) {
				throw new Exception(e);
			}

		}
		return result;
	}
	/**
	 * Gets the rest template.
	 *
	 * @return the rest template
	 * @throws KeyManagementException   the key management exception
	 * @throws NoSuchAlgorithmException the no such algorithm exception
	 * @throws KeyStoreException        the key store exception
	 */
    public RestTemplate getRestTemplate() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
    	if (localRestTemplate == null) {
			HttpClientBuilder httpClientBuilder = HttpClients.custom().setMaxConnPerRoute(maxConnectionPerRoute)
					.setMaxConnTotal(totalMaxConnection).disableCookieManagement();
			HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
			requestFactory.setHttpClient(httpClientBuilder.build());
			localRestTemplate = new RestTemplate(requestFactory);
		}
		return localRestTemplate;
    }

	/**
	 * Sets the request header.
	 *
	 * @param requestType the request type
	 * @param mediaType   the media type
	 * @return the http entity
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    private HttpEntity<Object> setRequestHeader(Object requestType, MediaType mediaType) throws IOException {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
        headers.add("Cookie", getToken());
        if (mediaType != null) {
            headers.add("Content-Type", mediaType.toString());
        }
        if (requestType != null) {
            try {
                HttpEntity<Object> httpEntity = (HttpEntity<Object>) requestType;
                HttpHeaders httpHeader = httpEntity.getHeaders();
                Iterator<String> iterator = httpHeader.keySet().iterator();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    if (!(headers.containsKey("Content-Type") && key == "Content-Type"))
                        headers.add(key, httpHeader.get(key).get(0));
                }
                return new HttpEntity<Object>(httpEntity.getBody(), headers);
            } catch (ClassCastException e) {
                return new HttpEntity<Object>(requestType, headers);
            }
        } else
            return new HttpEntity<Object>(headers);
    }

	/**
	 * Gets the token.
	 *
	 * @return the token
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    public String getToken() throws IOException {
        String token = System.getProperty("token");
        boolean isValid = false;

        if (StringUtils.isNotEmpty(token)) {

			isValid = TokenHandlerUtil.isValidBearerToken(token,
					environment.getProperty("data.share.token.request.issuerUrl"),
					environment.getProperty("data.share.token.request.clientId"));


        }
        if (!isValid) {
            TokenRequestDTO<SecretKeyRequest> tokenRequestDTO = new TokenRequestDTO<SecretKeyRequest>();
			tokenRequestDTO.setId(environment.getProperty("data.share.token.request.id"));
            tokenRequestDTO.setMetadata(new Metadata());

            tokenRequestDTO.setRequesttime(DateUtils.getUTCCurrentDateTimeString());
            // tokenRequestDTO.setRequest(setPasswordRequestDTO());
            tokenRequestDTO.setRequest(setSecretKeyRequestDTO());
			tokenRequestDTO.setVersion(environment.getProperty("data.share.token.request.version"));

            Gson gson = new Gson();
            HttpClient httpClient = HttpClientBuilder.create().build();
            // HttpPost post = new
            // HttpPost(environment.getProperty("PASSWORDBASEDTOKENAPI"));
            HttpPost post = new HttpPost(environment.getProperty("KEYBASEDTOKENAPI"));
            try {
                StringEntity postingString = new StringEntity(gson.toJson(tokenRequestDTO));
                post.setEntity(postingString);
                post.setHeader("Content-type", "application/json");
                HttpResponse response = httpClient.execute(post);
                org.apache.http.HttpEntity entity = response.getEntity();
                String responseBody = EntityUtils.toString(entity, "UTF-8");
                Header[] cookie = response.getHeaders("Set-Cookie");
                if (cookie.length == 0)
                    throw new IOException("cookie is empty. Could not generate new token.");
                token = response.getHeaders("Set-Cookie")[0].getValue();
                System.setProperty("token", token.substring(14, token.indexOf(';')));
                return token.substring(0, token.indexOf(';'));
            } catch (IOException e) {
                throw e;
            }
        }
        return AUTHORIZATION + token;
    }

	/**
	 * Sets the secret key request DTO.
	 *
	 * @return the secret key request
	 */
    private SecretKeyRequest setSecretKeyRequestDTO() {
        SecretKeyRequest request = new SecretKeyRequest();
		request.setAppId(environment.getProperty("data.share.token.request.appid"));
		request.setClientId(environment.getProperty("data.share.token.request.clientId"));
		request.setSecretKey(environment.getProperty("data.share.token.request.secretKey"));
        return request;
    }

	/**
	 * Sets the password request DTO.
	 *
	 * @return the password request
	 */
    private PasswordRequest setPasswordRequestDTO() {
        PasswordRequest request = new PasswordRequest();
		request.setAppId(environment.getProperty("data.share.token.request.appid"));
		request.setPassword(environment.getProperty("data.share.token.request.password"));
		request.setUserName(environment.getProperty("data.share.token.request.username"));
        return request;
    }
}
