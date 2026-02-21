
#!/usr/bin/env bash
echo "Starting local CGI server on http://localhost:8000"
echo "Main app:   http://localhost:8000/cgi-bin/Prac_1.cgi"
echo "Test page:  http://localhost:8000/cgi-bin/test.cgi"

cd dist || { echo "Error: dist/ not found - run ./build.sh first"; exit 1; }

python3 -m http.server --cgi 8000
