FROM fnichol/uhttpd
MAINTAINER kp@scalytica.net
EXPOSE 80
COPY index.html /www/symbiotic/index.html
COPY js/ /www/symbiotic/js/
COPY resources/ /www/symbiotic/resources/
ENTRYPOINT ["/usr/sbin/run_uhttpd", "-f", "-p", "80", "-h", "/www"]
CMD [""]