---
name: alm-processing
description: Use this to perform operations on the langgraph4j project.  1) create a new project release 2) move to next dev release
---

# create a new project release

In this project we follow the "git flow" branching strategy. 
To create a new release we need to start from `develop` branch and accomplish the workflow outlined below:

## 1. Create a ad-hoc release branch.
Starting from `develop` branch and runs the command `git flow release start "<version>"` that will create a branch from `develop` named `release\<version>`.
## 2. Update project release 
After created successfully the release branch we must update the java project release runs the command `mvn versions:set -DnewVersion=<version>`. 
After the command successfully completed we must to confirm changes in `pom.xml` files runs command `mvn version:commit`
## 3. commit the release update
After update java project release we must commit to git runs commands
```
git add . ; git commit -m'build: bump to next version <version>'
```
## 4. wait for confirmation
At this point pause execution and ask to the user when ready to close the release, When he confirms proceed to next steps
## 5. close release
To close the release runs `git flow release finish "<version>" -m"new release <version>"` that will merge the branch `release/<version>` to both `main` and `develop` ones 


