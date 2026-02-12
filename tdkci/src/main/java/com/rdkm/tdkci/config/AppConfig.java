package com.rdkm.tdkci.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.rdkm.tdkci.utils.Constants;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletContext;

@Configuration
public class AppConfig {

	// Static variable that stores the servlet context path
	private static String realPath;

	// Base location of the application where configs are stored
	@Value("${tdkservice.baselocation}")
	private static String baselocation;

	// Servlet context
	private final ServletContext servletContext;

	/**
	 * Constructor injection and it is injected as dependency
	 * 
	 * @param servletContext
	 */
	public AppConfig(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	/**
	 * The @PostConstruct annotation ensures the init method is called after the
	 * bean is constructed.
	 */
	@PostConstruct
	public void init() {
		realPath = servletContext.getRealPath("/");
	}

	/**
	 * The getRealPath method provides a static access point to the cached real
	 * path.
	 * 
	 * @return realpath - It will be the path to webapps folder in case of
	 *         development run and base directory in case of standalone deployment
	 *         in webservers
	 * 
	 */
	public static String getRealPath() {
		return realPath;
	}

	/**
	 * The getBaselocation method provides a static access point to the base
	 * location of the application where the scripts and other configs are stored.
	 * If the base location is not set, it will be set using the real path and the
	 * base filestore directory.
	 * 
	 * @return baselocation - the base location of the application where the scripts
	 *         and other configs are stored
	 */
	public static String getBaselocation() {
		if (baselocation == null) {
			baselocation = getRealPath() + Constants.BASE_FILESTORE_DIR;
		}
		return baselocation;
	}

	@Bean
	RestTemplate restTemplate() {
		return new RestTemplate();
	}

}
