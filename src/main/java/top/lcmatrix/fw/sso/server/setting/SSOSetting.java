package top.lcmatrix.fw.sso.server.setting;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="top.lcmatrix.fw.sso")
public class SSOSetting {

	private String urlEncoding = "UTF-8";
	
	private String loginPage;

	public String getUrlEncoding() {
		return urlEncoding;
	}

	public void setUrlEncoding(String urlEncoding) {
		this.urlEncoding = urlEncoding;
	}

	public String getLoginPage() {
		return loginPage;
	}

	public void setLoginPage(String loginPage) {
		this.loginPage = loginPage;
	}
}
