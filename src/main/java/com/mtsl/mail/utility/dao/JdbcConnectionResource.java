package com.mtsl.mail.utility.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import jakarta.annotation.Resource;

public abstract class JdbcConnectionResource {
	@Resource
	protected JdbcTemplate jdbcTemplate;
	@Resource
	protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;
}
