package top.lcmatrix.fw.sso.server.listener;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletContext;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.Response;
import top.lcmatrix.fw.sso.server.Constant;
import top.lcmatrix.fw.sso.server.util.OkHttpUtil;
import top.lcmatrix.fw.sso.server.util.UrlUtil;

/**
 * 注销时移除保存的相关数据，同时调用各sso-client的注销接口
 * @author chris
 *
 */
@WebListener
public class SSOSessionListener implements HttpSessionListener{
	
	private static final Logger logger = LoggerFactory.getLogger(SSOSessionListener.class);
	
	private ExecutorService threadPool = Executors.newCachedThreadPool();
	
	@Override
	public void sessionCreated(HttpSessionEvent arg0) {
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent arg0) {
		HttpSession session = arg0.getSession();
		String token = (String) session.getAttribute(Constant.SESSION_TOKEN);
		if(StringUtils.isNotEmpty(token)){
			ServletContext servletContext = session.getServletContext();
			@SuppressWarnings("unchecked")
			Set<String> clientBaseUrls = (Set<String>) servletContext
					.getAttribute(Constant.CONTEXT_ATTR_CLIENT_BASE_URLS);
			if(clientBaseUrls != null && !clientBaseUrls.isEmpty()){
				//注销所有相关系统
				for(String clientBaseUrl : clientBaseUrls){
					logoutAsync(clientBaseUrl, token);
				}
			}
			session.getServletContext().removeAttribute(Constant.CONTEXT_ATTR_TOKEN_PREFIX + token);
		}
	}

	private void logoutAsync(String clientBaseUrl, String token){
		threadPool.submit(new Runnable() {
			@Override
			public void run() {
				String logoutUrl = UrlUtil.concatUrl(clientBaseUrl, Constant.SSO_CLIENT_PATH, Constant.ENDPOINT_CLIENT_LOGOUT);
				Map<String, String> params = new HashMap<>();
				params.put("token", token);
				Response response = null;
				try {
					response = OkHttpUtil.post(logoutUrl, params);
					if(!response.isSuccessful()){
						logger.error("单点注销失败，url：" + logoutUrl + "；token：" + token + "；status：" + response.code());
					}
				} catch (IOException e) {
					logger.error("单点注销失败，url：" + logoutUrl + "；token：" + token, e);
				} finally {
					if(response != null){
						response.close();
					}
				}
			}
		});
	}
}
