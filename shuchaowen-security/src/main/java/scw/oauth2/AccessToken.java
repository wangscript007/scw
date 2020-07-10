package scw.oauth2;

import java.io.Serializable;

import scw.mapper.Cloneable;
import scw.security.Token;

public class AccessToken implements Serializable, Cloneable {
	private static final long serialVersionUID = 1L;
	private final Token accessToken;
	private final TokenType tokenType;
	private final Token refreshToken;
	private final String scope;
	private final String state;

	public AccessToken(AccessToken accessToken) {
		this.accessToken = accessToken.accessToken;
		this.tokenType = accessToken.tokenType;
		this.refreshToken = accessToken.refreshToken;
		this.scope = accessToken.scope;
		this.state = accessToken.state;
	}

	public AccessToken(Token accessToken, TokenType tokenType, Token refreshToken, String scope, String state) {
		this.accessToken = accessToken;
		this.tokenType = tokenType;
		this.refreshToken = refreshToken;
		this.scope = scope;
		this.state = state;
	}

	public Token getAccessToken() {
		return accessToken;
	}

	public TokenType getTokenType() {
		return tokenType;
	}

	public Token getRefreshToken() {
		return refreshToken;
	}

	public String getScope() {
		return scope;
	}

	public String getState() {
		return state;
	}

	@Override
	public AccessToken clone() {
		return new AccessToken(accessToken.clone(), tokenType, refreshToken.clone(), scope, state);
	}

	@Override
	public String toString() {
		return "accessToken=[" + accessToken + "], tokenType=[" + tokenType + "], refreshToken=[" + refreshToken
				+ "], scope=" + scope + ", state=" + state;
	}
}
