# spring-oAuth2-SAML-integration [![Build Status](https://travis-ci.org/OhadR/spring-oAuth2-SAML-integration.svg?branch=master)](https://travis-ci.org/OhadR/spring-oAuth2-SAML-integration)   

How to integrate Spring-oAuth2 with Spring-SAML

Full article is here:
https://www.codeproject.com/Articles/598581/How-to-integrate-Spring-oAuth-with-Spring-SAML


# Introduction 
This document describes how to integrate the Spring-Security-oAuth2 project with Spring-Security-SAML.

I assume the reader is familiar with both oAuth and its components, and SAML and its components.

## 04-2018: Spring Versions Updated

On 04-2018, we have updated Spring versions.

* Spring Security: 4.0.3.RELEASE
* Spring: 4.2.4.RELEASE
* Spring Security oAuth: 2.0.9.RELEASE


If you wish to use the older version, (3.1.X, oAuth 1.0.5), you can find it on a separated branch. The version in that branch is 1.6.0. The version on Master is 2.0.0-SNAPSHOT.

# Motivation

Suppose you want your system to support oAuth2.  I would recommend using the Spring-Security-oAuth project. When you use Spring, you enjoy the many benefits of this open-source package: it is widely used, there is responsive support (in the forum), it is open source, and much more. This package allows the developer to write an oAuth-client, an oAuth resource server, or an oAuth authorization server.

Let us discuss SAML.  If you want to implement your own SAML SP (Service Provider), I recommend using Spring-Security-SAML, for the same reasons I recommended Spring-security-oAuth, above.

Now, consider an application that authenticates its users with oAuth, meaning the application is an "oAuth resource server", and its clients implement the oAuth protocol, meaning they are "oAuth clients".  I was asked to enable this application to connect SAML IdPs (identity providers) and authenticate users in front of them. This means the application must support not only oAuth, but SAML as well. Note, however, that if the application supports SAML, changes would have to be made in all clients, not only in the application itself. Currently the clients are "oAuth clients", (i.e., they fulfill the oAuth protocol). If the application supports SAML as well, the clients will also have to support it on their side. In SAML, the redirects are implemented differently, and the requests are different. So, the question is, how can we make this application support SAML without changing all clients?

The solution is to create an application ("the bridge") that will be a bridge between oAuth and SAML. When a non-authorized client tries to access the protected resource, it is redirected to the authorization server (this is how oAuth works). But here is the trick: from the client’s point of view– and from the application itself – this bridge functions as a valid "oAuth authorization server". Therefore, there is no need to change anything, not in the client code and not in the application code. On the other hand, instead of opening a popup dialog with username and password, this server functions as an SP and redirects the user to authenticate in front of a pre-configured IdP. 
   
# Important Notice #
This servers acts as :
 * Oauth authorization server => redirects to the IDP for authentication, gives authorization to resources and generates access tokens.
 * Resource server => IndexController.java is the resource to be authorized and accessed by the client.
 * Bridge between Oauth server part and SAML IDP part.

# Create DB Schema #
 Below the SQL to execute for DB Schema creation. You have to choose between Mysql or PostgreSQL.
 > Whatever the SGB you select, the java source code remains the same.
 
## Store SAML messages on db : ##
#### On MySQL ####

    CREATE TABLE saml_message_store (
      message_id varchar(255) PRIMARY KEY,
      message blob
    );
    
#### On PostgreSQL ####

    CREATE TABLE public.saml_message_store
    (
      message_id varchar(255) PRIMARY KEY,
      message bytea
    );
    
## Store of OAUTH2 clients registration on DB : ##
#### On MySQL And PostgreSQL ####

    create table oauth_client_details (
        client_id VARCHAR(256) PRIMARY KEY,
        resource_ids VARCHAR(256),
        client_secret VARCHAR(256),
        scope VARCHAR(256),
        authorized_grant_types VARCHAR(256),
        web_server_redirect_uri VARCHAR(256),
        authorities VARCHAR(256),
        access_token_validity INTEGER,
        refresh_token_validity INTEGER,
        additional_information VARCHAR(4096),
        autoapprove VARCHAR(256)
    );
   
## Create the OAUTH2 client on DB ##

Here we create a first client for the client web app on the DB. 
You can execute this query to create more clients on the DB.
 > In order to be granted to access the resource server, all client apps have to be inserted to the DB with this SQL script.

    INSERT INTO oauth_client_details
        (client_id, client_secret, scope, authorized_grant_types,
        web_server_redirect_uri, authorities, access_token_validity,
        refresh_token_validity, additional_information, autoapprove)
    VALUES
        ('samlOauthClientId', 'secret', 'read,write,trust',
        'refresh_token,authorization_code,client_credentials,implicit,password', null, 'ROLE_CLIENT', 100000, 100000, null, true);
       
## Store OAuth2 token do DB ##
#### On MySQL ####

    CREATE TABLE oauth_access_token (
        token_id VARCHAR(256),
        token blob,
        authentication_id VARCHAR(256),
        user_name VARCHAR(256),
        client_id VARCHAR(256),
        authentication blob,
        refresh_token VARCHAR(256)
    );
    
    CREATE TABLE oauth_refresh_token (
        token_id VARCHAR(256),
        token blob,
        authentication blob
    );
    
    CREATE TABLE oauth_code (
        code VARCHAR(256), authentication blob
    );

#### On PostgreSQL ####
    
    CREATE TABLE oauth_access_token (
        token_id VARCHAR(256),
        token bytea,
        authentication_id VARCHAR(256),
        user_name VARCHAR(256),
        client_id VARCHAR(256),
        authentication bytea,
        refresh_token VARCHAR(256)
    );
        
    CREATE TABLE oauth_refresh_token (
        token_id VARCHAR(256),
        token bytea,
        authentication bytea
    );
    
    CREATE TABLE oauth_code (
        code VARCHAR(256), authentication bytea
    );
    
# Generate sign keystore #
 Open a command line and type this command (Change to your favorite location for the argument -keystore) :

    keytool -genkeypair -alias ohadr -dname cn=localhost -validity 365 -keyalg DSA -keysize 1024 -keypass kspass123 -storetype jceks -keystore C:/WORK/PERSO/spring-saml-workspace/keystore.jck -storepass kspass123

# Configuration #

All configuration goes to default.properties file :

 ### Database connection ###

    DB_HOST=localhost
    DB_PORT=5432
    DB_SCHEMA=samldb
    MARS_DB_USER=samlusr
    MARS_DB_PASSWORD=samlusrpwd

  > for MYSQL DB_PORT=PORT 3306, for POSGRESQL PORT DB_PORT=5432
  
  ### Cryptographic settings ###
  
    com.ohadr.crypto.keystore=C:/WORK/PERSO/spring-saml-workspace/keystore.jck
    com.ohadr.crypto.password=kspass123
    com.ohadr.crypto.keyAlias=ohadr
    
  * com.ohadr.crypto.keystore : this is same than the path that you gave for keystore generation.
  * com.ohadr.crypto.password : same thing thant keystore generation
  * com.ohadr.crypto.keyAlias : same thing thant keystore generation

 ### Token parameters ##
 
    com.ohadr.oauth2.token.issuer=com.ohadr.shalom
    com.ohadr.oauth2.token.timeToLive=10
    com.ohadr.oauth2.token.refreshTimeToLive=10
    
 * com.ohadr.oauth2.token.issuer : the displayed issuer for the client
 * com.ohadr.oauth2.token.timeToLive : token time to live in seconds
 * com.ohadr.oauth2.token.refreshTimeToLive : token refresh frequency time in seconds.
 


# How to build? #

	mvn clean install.

# How to run? #

The easiest way is to use tomcat-maven-plugin, by 
    
	...\>mvn tomcat7:run

# How to get this server Metadata #

Call this URL on you web browser, the metadata of this SP will be displayed. You can then register these metadata on an Identity provider.

    http://localhost:8080/oauth-2-saml/saml/metadata

# Questions?

Feel free to open issues here if you have any unclear matter or any other question.
