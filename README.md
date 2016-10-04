# Wildfly Deployments Tools

## Apps:

- Branch synchronizer app
- Artifact deployment app
- Slave safeguard app
- Integrity check for environmnent slaves

## 1. Branch synchronizer app

Our developers have an ability to deploy feature branches to many our environments.
But with great power comes great responsibility so we have to automatically clean that and remove
branches we don't need anymore.

To solve that issue we have a few Java apps that work together

This one does:

- scan specified folders searching for WAR files
- get a list of Wildfly domain controllers for specified environment and stability from AWS and get a list
of deployed applications
- compare two lists, ecking of checksum, creating lists of WAR files to be deployed and undeployed
- deploy/undeploy things for all domain controllers for that environment/stability

How to run:

```
java -jar wildfly-sync.jar \
    -Djavax.net.ssl.trustStore="C:\whatever\cacerts" \
    -DwildflyPassword=seG^&R(a \
    -DwildflyLogin=wildflyadmin \
    -Denvironment=uat \
    -Dstability=stable \
    -DlocalLibraryPath=C:\tmp\wars,C:\tmp\wars\release \
    -DawsAccessKeyId=AAAAAABBBJJ999AA000AA \
    -DawsSecretAccessKey=111111122233344444/aaa444XXXQQQT33311111 \
    -DdryRun=true \
    -DignoreArtifacts=activemq-rar-5.13.0.rar
```

## 2. Artifact deployment app

This app deploys artifact to multiple Wildfly domain controllers. You don't need to specify exact IP addresses
of those things but AWS environment/stability tags

How to run:

```
java -jar wildfly-deploy.jar \
    -Djavax.net.ssl.trustStore="C:\whatever\cacerts" \
    -DwildflyPassword=seG^&R(a \
    -DwildflyLogin=wildflyadmin \
    -Denvironment=uat \
    -Dstability=stable \
    -Ddeployment=C:\tmp\wars\life-express.war \
    -DawsAccessKeyId=AAAAAABBBJJ999AA000AA \
    -DawsSecretAccessKey=111111122233344444/aaa444XXXQQQT33311111 \
    -DdryRun=true \
```

## 3. Slave safeguard app

Sometimes we have situation when both slaves are alive and visible but Wildfly domain controller
reports that deployments aren't running anywhere for some reasons

This app is a workaround that checking if slaves are actually active and ready to serve content
and if they aren't - restarts them

How to run:

```
java -jar wildfly-safeguard.jar \
    -Djavax.net.ssl.trustStore="C:\whatever\cacerts" \
    -DwildflyPassword=seG^&R(a \
    -DwildflyLogin=wildflyadmin \
    -Denvironment=uat \
    -Dstability=stable \
```

## 4. Integrity check

This application queries all the slaves across all domain controllers for specified
environment/colour and checks that all deployments are identical across all the slaves

How to run:

```
java -jar wildfly-integrity-check.jar \
    -Djavax.net.ssl.trustStore="C:\whatever\cacerts" \
    -DwildflyPassword=seG^&R(a \
    -DwildflyLogin=wildflyadmin \
    -Denvironment=uat \
    -Dstability=stable \
```

## Parameters

### Both apps have these system properties

To trust our Wildfly servers self-signed sertificates you may need to pass `javax.net.ssl.trustStore`
property with the path to a certificate store

#### wildflyPassword *
Password for Wildfly domain controller management user

#### wildflyLogin
Login for Wildfly domain controller management user
Default: `agwildflyadmin`

#### awsAccessKeyId, awsSecretAccessKey
AWS credentials to use for discovery of Wildfly domain controllers. Application will use default credentials
provided in .aws/credentials if those params were not specified

#### dryRun
Do nothing if this parameter set to `true` and just produce output about operations
Default: `false`

#### environment
AWS environment to use during apps' sync. Default implementation uses `Environment` tag
Default: `int`

#### stability
AWS stability name to use during apps' sync. We use colours tags in AWS and there is a builtin colour resolver
(static for now) which resolves `stable` to `Color:green` and `unstable` to `Color:blue` tags:values
Default: `unstable`

### Branch synchronizer app specific system properties

#### localLibraryPath
Path to local folder with .war files (applications)
Default: `./tmp`

#### ignoreArtifacts
List of ignored artifacts divided by ","
Default: `activemq-rar-5.13.0.rar`, `services.war`

#### disableDeploy
Boolean value. Syncronizer will not deploy artifacts to remote hosts if flag set to true
Default: `false`

#### disableUndeploy
Boolean value. Syncronizer will not undeploy artifacts from remote hosts if flag set to true
Default: `false`

### Artifact deployment app

#### deployment
Path to local artifact. Can be a folder - in this case first file found in that folder and its subfolders
mathing pattern .war|.ear will be deployed
Default: `./`


# Development

Read CONTRIBUTING.md to learn more about contributing to this project.

## How to setup and build

AWS parts requires credentials. Here is a short guide from Amazon how to do that:
http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/set-up-creds.html

This application uses some AWS EC2 tags to find Wildfly domain controller instances:

- `Environment` and `Color` tags to find environment for syncronization
- `Role` tag with value `wildfly-domain` to find instances with domain controllers within environments

To build an app run:

```
./gradlew clean check shadowJar
```

## Publishing core

Core package can be published using those command:

```
./gradlew.bat core:publish -Pusername=user -Ppassword=passw -Pnexus=path-to-local-nexus:8081
```

## License

The MIT License (MIT)

Copyright (c) 2016 Auto & General Insurance Company Ltd.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.



