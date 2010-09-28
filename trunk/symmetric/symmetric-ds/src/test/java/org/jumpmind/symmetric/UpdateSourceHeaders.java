/*
 * Licensed to JumpMind Inc under one or more contributor 
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding 
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU Lesser General Public License (the
 * "License"); you may not use this file except in compliance
 * with the License. 
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see           
 * <http://www.gnu.org/licenses/>.
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.  */
package org.jumpmind.symmetric;

import java.io.File;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

public class UpdateSourceHeaders {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        Collection<File> files = (Collection<File>) FileUtils.listFiles(new File("src"),
                new String[] { "java" }, true);
        for (File file : files) {
            updateHeader(file, FileUtils.readFileToString(new File("HEADER.txt")));
        }

    }
    
    private static void updateHeader(File file, String newHeaderTxt) throws Exception {
        StringBuilder newContents = new StringBuilder(FileUtils.readFileToString(file).trim());        
        if (newContents.toString().startsWith("/*")) {
            String oldHeader = newContents.toString().substring(0, newContents.toString().indexOf("*/"));
            newContents = new StringBuilder(newContents.toString().substring(newContents.toString().indexOf("*/") + 2));
            Pattern pattern = Pattern.compile("Copyright.*");
            Matcher matcher = pattern.matcher(oldHeader);
            while (matcher.find()) {
                String group = matcher.group();
                String author = group.substring("Copyright (C) ".length());
                insertAuthor(file.getName(), newContents, author);
            }
        }
        
        FileUtils.writeStringToFile(file, newHeaderTxt+"\n"+newContents.toString());

    }
    
    private static void insertAuthor(String fileName, StringBuilder contents, String author) {
        int classBeginIndex = contents.indexOf("class " + fileName.substring(0, fileName.length()-5));
        if (classBeginIndex < 0) {
            classBeginIndex = contents.indexOf("interface " + fileName.substring(0, fileName.length()-5));            
        }               
        if (classBeginIndex < 0) {
            classBeginIndex = contents.indexOf("enum " + fileName.substring(0, fileName.length()-5));            
        }                   
        
        if (classBeginIndex < 0) {
            throw new IllegalStateException("Could not find class start: " + contents);
        } else {
            if (contents.substring(0, classBeginIndex).lastIndexOf("final") > 0) {
                classBeginIndex = contents.substring(0, classBeginIndex).lastIndexOf("final");
            }
            if (contents.substring(0, classBeginIndex).lastIndexOf("public") > 0) {
                classBeginIndex = contents.substring(0, classBeginIndex).lastIndexOf("public");
            }
            if (contents.substring(0, classBeginIndex).lastIndexOf("abstract") > 0) {
                classBeginIndex = contents.substring(0, classBeginIndex).lastIndexOf("abstract");
            }
            
            int insertPoint = contents.substring(0, classBeginIndex+1).indexOf("*/")-1;
            if (insertPoint < 0) {
                contents.insert(classBeginIndex, " */\n");
                contents.insert(classBeginIndex, " * @author " + author + "\n");
                contents.insert(classBeginIndex, "/**\n");                
            } else {
                contents.insert(insertPoint, " * @author " + author + "\n");
                contents.insert(insertPoint, " *\n");                
            }
        }
    }
}