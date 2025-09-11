/*
* If not stated otherwise in this file or this component's Licenses.txt file the
* following copyright and licenses apply:
*
* Copyright 2024 RDK Management
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*
http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.rdkm.tdkservice.model;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.rdkm.tdkservice.enums.Category;
import com.rdkm.tdkservice.enums.Theme;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * User entity class
 */
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@Table(name = "user", uniqueConstraints = { @UniqueConstraint(columnNames = "username"),
		@UniqueConstraint(columnNames = "email") })
public class User extends BaseEntity implements UserDetails {

	private static final long serialVersionUID = 1L;

	/*
	 * The User name of the user.
	 */

	@Column(nullable = false)
	private String username;

	/**
	 * The password of the user.
	 */
	private String password;

	/**
	 * The email of the user.
	 */
	private String email;

	/**
	 * The display name of the user.
	 */
	private String displayName;

	/**
	 * The status of the user.
	 */
	private String status;

	/**
	 * The theme of the user.
	 */
	@Enumerated(EnumType.STRING)
	private Theme theme;

	/*
	 * The category
	 * 
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Category category;

	/**
	 * The user group of the user.
	 */
	@ManyToOne(cascade = CascadeType.PERSIST)
	@JoinColumn(name = "user_group_id")
	private UserGroup userGroup;

	/**
	 * The user role of the user.
	 */
	@ManyToOne(cascade = CascadeType.PERSIST)
	@JoinColumn(name = "user_role_id")
	private UserRole userRole;

	/**
	 * This method is used to get the authorities granted to the user.
	 */
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority(userRole.getName()));
	}

	/**
	 * This method is used to get the username of the user.
	 */
	@Override
	public String getUsername() {
		return username;
	}

	/**
	 * Indicates whether the user's account has expired.
	 *
	 * @return true if the user's account is valid (non-expired), false if no longer
	 *         valid
	 *
	 *         Note: Actual account expiration logic is not implemented here; always
	 *         returns true.
	 */
	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	/**
	 * Indicates whether the user is locked or unlocked.
	 *
	 * @return true if the user is not locked, false otherwise
	 *
	 *         Note: Actual account lock logic is not implemented here; always
	 *         returns true.
	 */
	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	/**
	 * Indicates whether the user's credentials (password) have expired.
	 *
	 * @return true if the user's credentials are valid (non-expired), false
	 *         otherwise
	 *
	 *         Note: Actual credential expiration logic is not implemented here;
	 *         always returns true.
	 */
	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	/**
	 * Indicates whether the user is enabled or disabled.
	 *
	 * @return true if the user is enabled, false otherwise
	 *
	 *         Note: Actual enable/disable logic is not implemented here; always
	 *         returns true.
	 */
	@Override
	public boolean isEnabled() {
		return true;
	}

}
