package com.proxy.model;

public class GatewayProfile {
	  private String manufacturer; // Clear
	  private String deviceNo; // Clear
	  private String hardwareVersion; // Clear
	  private String softwareVersion; // Clear
	  private String softwareVersion2; // Clear
	  private CodecVersion dispatcherProtocolVersion; // Not present in DeviceData
	  private CodecVersion serviceProtocolVersion; // DeviceData has CodecVersion
	  private String encoding; // DeviceData has CodecVersion
	  private Integer encryptionAlgorithm; // DeviceData has serviceDataCryptoAlgorithmId and
	  // TransportDataCryptoAlgorithmId, not clear which one
	  private String
	      encryptionKey; // DeviceData has TransportDataCryptoKey and ServiceDataCryptoKey, they both
	  // use integer isntead of string

}
