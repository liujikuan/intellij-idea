{
  "AWSTemplateFormatVersion" : "2010-09-09",

  "Description" : "CloudFormation template to create a VPC with a public subnet on EC2",

  "Parameters" : {
  
    "StackName" : {
      "Description" : "Name of the CloudFormation stack that is used to tag instances",
      "Type" : "String",
      "MinLength": "1",
      "MaxLength": "50"
    },
    
    "StackOwner" : {
      "Description" : "The instances will have this parameter as an Owner tag.",
      "Type" : "String",
      "MinLength": "1",
      "MaxLength": "50"
    },

    "CBUserData" : {
      "Description" : "User data to be executed",
      "Type" : "String",
      "MinLength": "9",
      "MaxLength": "50000"
    },
    
    "KeyName": {
      "Description" : "Name of an existing EC2 KeyPair to enable SSH access to the instances",
      "Type": "String",
      "MinLength": "1",
      "MaxLength": "255",
      "AllowedPattern" : "[\\x20-\\x7E]*",
      "ConstraintDescription" : "can contain only ASCII characters."
    },

    "AMI" : {
      "Description" : "AMI that's used to start instances",
      "Type" : "String",
      "MinLength": "12",
      "MaxLength": "12",
      "AllowedPattern" : "ami-[a-z0-9]{8}",
      "ConstraintDescription" : "must follow pattern: ami-xxxxxxxx"
    },

    "RootDeviceName" : {
      "Description" : "Name of the root device that comes with the AMI",
      "Type" : "String",
      "MinLength": "8",
      "MaxLength": "12"
    }

  },

  "Mappings" : {
    "SubnetConfig" : {
      "VPC"     : { "CIDR" : "${cbSubnet}" },
      "Public"  : { "CIDR" : "${cbSubnet}" }
    }
  },

  "Resources" : {

    "VPC" : {
      "Type" : "AWS::EC2::VPC",
      "Properties" : {
        "CidrBlock" : { "Fn::FindInMap" : [ "SubnetConfig", "VPC", "CIDR" ]},
        "EnableDnsSupport" : "true",
        "EnableDnsHostnames" : "true",
        "Tags" : [
          { "Key" : "Application", "Value" : { "Ref" : "AWS::StackId" } },
          { "Key" : "Network", "Value" : "Public" }
        ]
      }
    },

    "PublicSubnet" : {
      "Type" : "AWS::EC2::Subnet",
      "Properties" : {
        "VpcId" : { "Ref" : "VPC" },
        "CidrBlock" : { "Fn::FindInMap" : [ "SubnetConfig", "Public", "CIDR" ]},
        "Tags" : [
          { "Key" : "Application", "Value" : { "Ref" : "AWS::StackId" } },
          { "Key" : "Network", "Value" : "Public" }
        ]
      }
    },

    "InternetGateway" : {
      "Type" : "AWS::EC2::InternetGateway",
      "Properties" : {
        "Tags" : [
          { "Key" : "Application", "Value" : { "Ref" : "AWS::StackId" } },
          { "Key" : "Network", "Value" : "Public" }
        ]
      }
    },

    "AttachGateway" : {
       "Type" : "AWS::EC2::VPCGatewayAttachment",
       "Properties" : {
         "VpcId" : { "Ref" : "VPC" },
         "InternetGatewayId" : { "Ref" : "InternetGateway" }
       }
    },

	
    "PublicRouteTable" : {
      "Type" : "AWS::EC2::RouteTable",
      "Properties" : {
        "VpcId" : { "Ref" : "VPC" },
        "Tags" : [
          { "Key" : "Application", "Value" : { "Ref" : "AWS::StackId" } },
          { "Key" : "Network", "Value" : "Public" }
        ]
      }
    },

    "PublicRoute" : {
      "Type" : "AWS::EC2::Route",
      "DependsOn" : "AttachGateway",
      "Properties" : {
        "RouteTableId" : { "Ref" : "PublicRouteTable" },
        "DestinationCidrBlock" : "0.0.0.0/0",
        "GatewayId" : { "Ref" : "InternetGateway" }
      }
    },

    "PublicSubnetRouteTableAssociation" : {
      "Type" : "AWS::EC2::SubnetRouteTableAssociation",
      "Properties" : {
        "SubnetId" : { "Ref" : "PublicSubnet" },
        "RouteTableId" : { "Ref" : "PublicRouteTable" }
      }
    },
    <#list templates as tgroup>
	"AmbariNodes${tgroup.groupName?replace('_', '')}" : {
      "Type" : "AWS::AutoScaling::AutoScalingGroup",
      "DependsOn" : "PublicSubnet",
      "Properties" : {
        "AvailabilityZones" : [{ "Fn::GetAtt" : [ "PublicSubnet", "AvailabilityZone" ] }],
        "VPCZoneIdentifier" : [{ "Ref" : "PublicSubnet" }],
        "LaunchConfigurationName" : { "Ref" : "AmbariNodeLaunchConfig${tgroup.groupName?replace('_', '')}" },
        "MinSize" : 1,
        "MaxSize" : ${tgroup.nodeCount},
        "DesiredCapacity" : ${tgroup.nodeCount},
        "Tags" : [ { "Key" : "Name", "Value" : { "Ref" : "StackName" }, "PropagateAtLaunch" : "true" },
        		   { "Key" : "owner", "Value" : { "Ref" : "StackOwner" }, "PropagateAtLaunch" : "true" },
        		   { "Key" : "hostGroup", "Value" : "${tgroup.groupName}", "PropagateAtLaunch" : "true" }]
      }
    },

    "AmbariNodeLaunchConfig${tgroup.groupName?replace('_', '')}"  : {
      "Type" : "AWS::AutoScaling::LaunchConfiguration",
      "Properties" : {
      	"BlockDeviceMappings" : [
      	  {
            "DeviceName" : { "Ref" : "RootDeviceName" },
            "Ebs" : {
              "VolumeSize" : "50",
              "VolumeType" : "gp2"
            }
          }, {
            "DeviceName" : "/dev/sdb",
            "NoDevice" : true,
            "Ebs": {}
      	  }, {
            "DeviceName" : "/dev/sdc",
            "NoDevice" : true,
            "Ebs": {}
          }, {
            "DeviceName" : "/dev/sdd",
            "NoDevice" : true,
            "Ebs": {}
          }, {
            "DeviceName" : "/dev/sde",
            "NoDevice" : true,
            "Ebs": {}
          }
		  <#assign seq = ["f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"]>
			<#list seq as x>
			<#if x_index = tgroup.template.volumeCount><#break></#if>
  		  ,{
          	"DeviceName" : "/dev/xvd${x}",
          	<#if tgroup.template.volumeType == "ephemeral">
            "VirtualName" : "ephemeral${x_index}"
            <#else>
            "Ebs" : {
              "VolumeSize" : ${tgroup.template.volumeSize},
              "VolumeType" : "${tgroup.template.volumeType}"
            }
            </#if>
      	  }
			</#list>
      	],
        "ImageId"        : { "Ref" : "AMI" },
        "SecurityGroups" : [ { "Ref" : "ClusterNodeSecurityGroup" } ],
        "InstanceType"   : "${tgroup.template.instanceType}",
        "KeyName"        : { "Ref" : "KeyName" },
        "AssociatePublicIpAddress" : "true",
        <#if useSpot>
        "SpotPrice"      : ${tgroup.template.spotPrice},
        </#if>
        "UserData"       : { "Fn::Base64" : { "Ref" : "CBUserData"}}
      }
    },
    </#list>

    "ClusterNodeSecurityGroup" : {
      "Type" : "AWS::EC2::SecurityGroup",
      "Properties" : {
        "GroupDescription" : "Allow access from web and bastion as well as outbound HTTP and HTTPS traffic",
        "VpcId" : { "Ref" : "VPC" },
        "SecurityGroupIngress" : [
          <#list subnets as s>
            <#list ports as p>
                { "IpProtocol" : "${p.protocol}", "FromPort" : "${p.localPort}", "ToPort" : "${p.port}", "CidrIp" : "${s.cidr}"} ,
            </#list>
		  </#list>
		  { "IpProtocol" : "icmp", "FromPort" : "-1", "ToPort" : "-1", "CidrIp" : "${cbSubnet}"} ,
          { "IpProtocol" : "tcp", "FromPort" : "0", "ToPort" : "65535", "CidrIp" : "${cbSubnet}"} ,
          { "IpProtocol" : "udp", "FromPort" : "0", "ToPort" : "65535", "CidrIp" : "${cbSubnet}"}
        ]
      }
    }
    
  },
  
  "Outputs" : {
  
	"Subnet" : {
	  "Value" : { "Ref" : "PublicSubnet" }
    },
    "SecurityGroup" : {
      "Value" : { "Ref" : "ClusterNodeSecurityGroup" }
    }
    
  }

}