package top.lcmatrix.fw.sso.server.controller;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import top.lcmatrix.fw.sso.server.Constant;
import top.lcmatrix.fw.sso.server.entity.TokenObject;
import top.lcmatrix.fw.sso.server.setting.SSOSetting;
import top.lcmatrix.fw.sso.server.util.UrlUtil;

@RequestMapping(Constant.SSO_SERVER_PATH)
@Controller
public class SSOServerController {
	
	@Autowired
	private SSOSetting ssoSetting;
	
	/**
	 * 登陆成功后，跳转到toUrl，并附上token
	 * @param loginUser
	 * @param toUrl		为null或空时不跳转
	 * @param response
	 * @throws IOException
	 */
	public static void onLoginSuccess(Object loginUser, String toUrl){
		HttpServletRequest request = request();
		String token = UUID.randomUUID().toString();
		String cookie = request.getHeader(Constant.COOKIE_IN_HEADER);
		session().setAttribute(Constant.SESSION_TOKEN, token);
		TokenObject tokenObject = new TokenObject(token, loginUser, cookie);
		saveTokenObject(token, tokenObject);
		
		if(StringUtils.isNotBlank(toUrl)){
			StringBuilder urlWithToken = new StringBuilder(toUrl);
			if(toUrl.contains("?")){
				urlWithToken.append("&" + Constant.PARAM_TOKEN + "=" + token);
			}else{
				urlWithToken.append("?" + Constant.PARAM_TOKEN + "=" + token);
			}
			try {
				response().sendRedirect(urlWithToken.toString());
			} catch (IOException e) {
				e.printStackTrace();
				try {
					response().sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "重定向失败");
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * 验证同步请求，已登录则跳转回来源url，同时附上token，否则跳转到登录页面
	 * @param returnUrl
	 * @return
	 */
	@GetMapping(Constant.ENDPOINT_SERVER_SYNC_REQUEST_VALIDATE)
	public String syncRequestValidate(String returnUrl) {
		HttpServletRequest request = request();
		String urlEncoding = ssoSetting.getUrlEncoding();
		
		if(StringUtils.isBlank(returnUrl)){
			return null;
		}else{
			returnUrl = UrlUtil.decode(returnUrl, urlEncoding);
			if (request.getSession().getAttribute(Constant.SESSION_TOKEN) == null) {
				return "redirect:" + ssoSetting.getLoginPage() + "?returnUrl=" + UrlUtil.encodeJustOnce(returnUrl, urlEncoding);
			}
			String token = (String) session().getAttribute(Constant.SESSION_TOKEN);
			String returnurlWithToken = returnUrl;
			if(returnUrl.contains("?")){
				returnurlWithToken += "&" + Constant.PARAM_TOKEN + "=" + token;
			}else{
				returnurlWithToken += "?" + Constant.PARAM_TOKEN + "=" + token;
			}
			return "redirect:" + returnurlWithToken;
		}
	}
	
	/**
	 * 验证异步请求（ajax），已登录则返回token object，否则返回空
	 * @param returnUrl
	 * @param baseUrl
	 * @return
	 */
	@RequestMapping(value = Constant.ENDPOINT_SERVER_AJAX_REQUEST_VALIDATE, produces = "application/json; charset=UTF-8")
	@ResponseBody
	public TokenObject ajaxRequestValidate(String returnUrl, String baseUrl) {
		HttpServletRequest request = request();
		String token = (String) request.getSession().getAttribute(Constant.SESSION_TOKEN);
		if (token == null) {
			return null;
		}
		saveBaseUrl(baseUrl, request);
		return getTokenObjectByToken(token);
	}
	
	/**
	 * 验证token是否合法
	 * @param token
	 * @param baseUrl
	 * @return
	 */
	@RequestMapping(value = Constant.ENDPOINT_SERVER_VALIDATETOKEN, produces = "application/json; charset=UTF-8")
	@ResponseBody
	public TokenObject validateToken(String token, String baseUrl) {
		if(StringUtils.isNotBlank(token)){
			HttpServletRequest request = request();
			TokenObject tokenObject = getTokenObjectByToken(token);
			
			if(tokenObject != null){
				saveBaseUrl(baseUrl, request);
				return tokenObject;
			}
		}
		return null;
	}
	
	/**
	 * 跳转至登录页面
	 * @param returnUrl
	 * @return
	 */
	@GetMapping(Constant.ENDPOINT_SERVER_LOGIN_PAGE)
	public String loginPage(String returnUrl) {
		String loginPage = ssoSetting.getLoginPage();
		if(loginPage.contains("?")){
			loginPage += "&returnUrl=" + returnUrl;
		}else{
			loginPage += "?returnUrl=" + returnUrl;
		}
		return "redirect:" + loginPage;
	}

	private void saveBaseUrl(String baseUrl, HttpServletRequest request) {
		if(StringUtils.isNotBlank(baseUrl)){
			ServletContext servletContext = request.getServletContext();
			@SuppressWarnings("unchecked")
			Set<String> clientBaseUrls = (Set<String>) servletContext
					.getAttribute(Constant.CONTEXT_ATTR_CLIENT_BASE_URLS);
			if(clientBaseUrls == null){
				clientBaseUrls = new HashSet<>();
				servletContext.setAttribute(Constant.CONTEXT_ATTR_CLIENT_BASE_URLS, clientBaseUrls);
			}
			clientBaseUrls.add(baseUrl);
		}
	}
	
	public static void saveTokenObject(String token, TokenObject tokenObject){
		request().getServletContext().setAttribute(Constant.CONTEXT_ATTR_TOKEN_PREFIX + token, tokenObject);
	}
	
	public static TokenObject getTokenObjectByToken(String token){
		return (TokenObject) request().getServletContext().getAttribute(Constant.CONTEXT_ATTR_TOKEN_PREFIX + token);
	}
	
	public static void removeTokenObject(String token){
		request().getServletContext().removeAttribute(Constant.CONTEXT_ATTR_TOKEN_PREFIX + token);
	}
	
	private static HttpServletRequest request(){
		return ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest();
	}
	
	private static HttpServletResponse response(){
		return ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getResponse();
	}
	
	private static HttpSession session(){
		return request().getSession();
	}
}
