package scw.oauth2.client;

import java.net.URL;

import scw.oauth2.AccessToken;

/**
 * 授权码模式（authorization code）是功能最完整、流程最严密的授权模式。<br/>
 * 它的特点就是通过客户端的后台服务器，与"服务提供商"的认证服务器进行互动。
 * 
 * @author shuchaowen
 *
 */
public interface AuthorizationCodeClient extends RefreshAccessTokenClient {
	String getClientId();

	URL getAuthorizeURL(String redirect_uri, String scope, String state);

	AccessToken getAccessToken(String code, String redirect_uri);
}
