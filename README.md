DocAnt
======
Description
------
This project conetains Ant tasks for [jdocbook](https://github.com/jeffzhang/jdocbook-core).

Setup
-----
* (install javasdk, ant and [ivy](http://ant.apache.org/ivy/))
* git clone git://github.com/jeffzhang/docant.git
* cd docant
* ant release
* cd target/release
* ant
* (got html, html-single, pdf files)

Download files
-----
It download some files include:

* docbook dtds, xls file
* dependend jars
* hacked jdocbook (in alpha phase)

Docbook files
-----
Currently it includes some documents:

* JBoss AS 6 (part)
* Ironjacamar (JBoss JCA)
* Hornetq
* Hibernate (some section missing)
* Spring (some section missing, so far "borrow" jboss jdocbook style)

Note
------
* Docbook files is from svn/git or latest release package file. 
* Change abstract path or DTD statement
* remove some section since it throw error when transform

Roadmap
-------
* Fix transform error
* Support more docbook files
* Automaticly grep neweast document and compare/merge
* support all jdocbook feature include multi-language (pot generate, po update/merge)
* sync with jdocbook upstream


