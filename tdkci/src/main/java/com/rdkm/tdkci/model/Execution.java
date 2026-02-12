package com.rdkm.tdkci.model;

import com.rdkm.tdkci.enums.AppType;
import com.rdkm.tdkci.enums.Category;
import com.rdkm.tdkci.enums.ExecutionStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "execution")
public class Execution extends BaseEntity {

	/*
	 * The CI request image name
	 */
	@Column(unique = true, nullable = false)
	private String requestImageName;

	/*
	 * The CI request image version
	 */
	@Column(unique = true, nullable = false)
	private String requestImageVersion;

	/*
	 * The upgrade image file name
	 */
	@Column(unique = true, nullable = false)
	private String upgradeImageFileName;

	/*
	 * App Type
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AppType appType = AppType.TDK;

	/*
	 * Execution Status
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ExecutionStatus executionStatus = ExecutionStatus.PENDING;

	/*
	 * Category of the execution
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Category category;

	/*
	 * 
	 */
	@ManyToOne(cascade = CascadeType.PERSIST)
	@JoinColumn(name = "device_id", nullable = false)
	private Device device;

}
