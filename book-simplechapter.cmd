ssh -i %HOMEPATH%/.ssh/vagrant_private_key vagrant@192.168.72.101 "/usr/local/rvm/gems/default/gems/asciidoctor-pdf-1.5.0.rc.2/bin/asciidoctor-pdf --trace -a pdf-style=/opt/dev/src/main/asciidoc/themes/basic-theme.yml -a pdf-fontsdir=/opt/dev/src/main/asciidoc/fonts -r asciidoctor-mathematical /opt/dev/src/main/asciidoc/book-simplechapter.asciidoc"