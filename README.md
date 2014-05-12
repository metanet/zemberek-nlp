Zemberek-NLP
============
This is a revamped version of Zemberek project. When it is used within a Solr Plugin, static resource files can't be read. Therefore, it has been modified for fixing the class loader issues that occur when reading static resource files. Usages of Google Guava's Resources class have been replaced with zemberek.core.io.ResourceUtil class.
Please note that, one my also fix the issue with copying the static resource files into appropriate container (i.e. Tomcat e.g.) directories.

You can access to the original code of the project via https://github.com/ahmetaa/zemberek-nlp

I demand no credits for the modifications on the original code since they have no value of contribution and I give no guarantee for my support.



