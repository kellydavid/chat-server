#!/bin/bash

printf 'Cleaning...\n\n'
mvn clean
printf 'Compiling...\n\n'
mvn compile
printf 'Packaging...\n\n'
mvn package
