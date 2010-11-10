# JDBC properties for Db2 UDB

# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# Use this property if ddlutils does not recognize the platform from the settings
#ddlutils.platform=Db2

# Properties starting with "datasource." will be fed into the datasource instance of the
# class configured via the datasource.class property

datasource.class=org.apache.commons.dbcp.BasicDataSource

datasource.driverClassName=COM.ibm.db2.jdbc.net.DB2Driver
datasource.url=jdbc:db2://localhost:6789/ddlutils
datasource.username=ddlutils
datasource.password=ddlutils
