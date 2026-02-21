# COS332
# Prac 1

## Prerequisites
- Java installed
- Apache2 installed (see below)

---

## Build Locally (Without Apache)

```bash
./build.sh
```

---

## Deploy with Apache

### 1. Install Apache
```bash
sudo apt install apache2
sudo a2enmod cgid
sudo a2enconf serve-cgi-bin
sudo systemctl restart apache2
```

### 2. Unzip submission
```bash
cd / && sudo unzip -o /path/to/submission.zip
```

### 3. Open in browser
```
http://localhost/cgi-bin/Prac_1.cgi
```

---

## Validate HTML5 Compliance

Redirect CGI output to files and upload to https://validator.w3.org/:

```bash
/usr/lib/cgi-bin/Prac_1.cgi > /tmp/home.html
/usr/lib/cgi-bin/Prac_1_Right.cgi > /tmp/right.html
/usr/lib/cgi-bin/Prac_1_Wrong.cgi > /tmp/wrong.html
```

---

## Cleanup

### Delete deployed files
```bash
sudo rm -r /usr/lib/cgi-bin/{Prac_1.cgi,Prac_1_Right.cgi,Prac_1_Wrong.cgi,test.cgi,classes} \
           $HOME/P1 \
           /tmp/home.html /tmp/right.html /tmp/wrong.html
```

### Verify deletion
```bash
ls /usr/lib/cgi-bin/ && ls $HOME/
```
