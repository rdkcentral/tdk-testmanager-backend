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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.rdkm.tdkservice.service.utilservices;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.regex.Pattern;

import org.apache.commons.validator.routines.InetAddressValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.rdkm.tdkservice.util.Constants;
import com.rdkm.tdkservice.util.Utils;

/**
 * The InetUtilityService class is for network related .
 *
 */
public class InetUtilityService {

	private static final Logger LOGGER = LoggerFactory.getLogger(InetUtilityService.class);

	// To store the IP address of the TM,stored as static variable
	// only one time IP address is fetched from the network interface and stored
	// so that for subsequent requests the IP address is returned from the static
	// variable
	public static String TM_IPV4_ADDRESS = "";
	public static String TM_IPV6_ADDRESS = "";

	/// Pattern for IPV6 address
	private static final Pattern IPV6_STD_PATTERN = Pattern.compile("^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");

	/**
	 * This method is used to check whether the given IP address is valid IPV4
	 * address.
	 * 
	 * @param ipAddress the IP address
	 * @return boolean
	 */
	public static boolean isIPv6Address(final String ipAddress) {
		return IPV6_STD_PATTERN.matcher(ipAddress).matches();
	}

	/**
	 * This method is used to get the IP address of the TM.
	 * 
	 * @param ipType          the IP type - IPV4 or IPV6
	 * @param nwInterfaceName the network interface name - eth0, eth
	 * @return String the IP address of the TM
	 */
	public static String getIPAddress(String ipType, String nwInterfaceName) {
		LOGGER.trace("getIPAddress method is called with ipType: {} and nwInterfaceName: {}", ipType, nwInterfaceName);
		String ipAddress = "";

		if (ipType.equals(Constants.IPV4)) {
			ipAddress = TM_IPV4_ADDRESS;
		} else if (ipType.equals(Constants.IPV6)) {
			ipAddress = TM_IPV6_ADDRESS;
		}
		try {
			/**
			 * If the IP address is not available in the static variable, fetch the IP
			 * address from the network interface and store it in the static variable
			 */
			if ((Utils.isEmpty(ipAddress)) && !Utils.isEmpty(nwInterfaceName)) {
				NetworkInterface configuredNetFace = NetworkInterface.getByName(nwInterfaceName);
				if (configuredNetFace != null) {
					ipAddress = setAndGetIPAddressFromNWInterface(configuredNetFace, ipType);
				}

			}

			/**
			 * If the IP address is not fetched w
			 */
			if (Utils.isEmpty(ipAddress)) {
				Enumeration<NetworkInterface> nwInterfacesAvaliable = NetworkInterface.getNetworkInterfaces();
				while (nwInterfacesAvaliable.hasMoreElements()) {
					NetworkInterface netFace = (NetworkInterface) nwInterfacesAvaliable.nextElement();

					ipAddress = setAndGetIPAddressFromNWInterface(netFace, ipType);
					if (!Utils.isEmpty(ipAddress)) {
						break;
					}

				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return ipAddress;
	}

	/**
	 * This method is used to set and get the IP address of the TM.
	 * 
	 * @param netFace the network interface
	 * @param ipType  the IP type - IPV4 or IPV6
	 * @return String the IP address of the TM
	 */
	private static String setAndGetIPAddressFromNWInterface(NetworkInterface netFace, String ipType) {
		String ipAddress = null;
		Enumeration<InetAddress> allInetAddress = netFace.getInetAddresses();

		while (allInetAddress.hasMoreElements()) {
			InetAddress address = (InetAddress) allInetAddress.nextElement();
			String hostAddr = address.getHostAddress();

			if (ipType.equals(Constants.IPV6) && hostAddr.contains(Constants.PERCENTAGE)) {
				hostAddr = hostAddr.substring(0, hostAddr.indexOf(Constants.PERCENTAGE));
			}
			if ((ipType.equals(Constants.IPV4) && InetAddressValidator.getInstance().isValidInet4Address(hostAddr))
					|| (ipType.equals(Constants.IPV6) && address instanceof Inet6Address
							&& StringUtils.countOccurrencesOf(hostAddr, Constants.COLON) > 4)) {
				if (!address.isLoopbackAddress() && !address.isLinkLocalAddress()) {
					ipAddress = hostAddr;
					if (ipType.equals(Constants.IPV4)) {
						TM_IPV4_ADDRESS = ipAddress;
					} else if (ipType.equals(Constants.IPV6)) {
						TM_IPV6_ADDRESS = ipAddress;
					}
					break;
				}
			}
		}

		return ipAddress;

	}
}
