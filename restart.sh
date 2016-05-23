#!/bin/sh
cd /www/www_soft/TestFileUpload
screen -d -m java  -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.Log4jLogDelegateFactory   
		-jar testfileupload-1.0.0-SNAPSHOT-fat.jar
