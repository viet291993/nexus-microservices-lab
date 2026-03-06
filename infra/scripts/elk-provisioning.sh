#!/bin/sh
set -e
apk add --no-cache curl jq || { echo "ERROR: Failed to install curl/jq"; exit 1; }

# Wait for Elasticsearch to be ready
echo "Waiting for Elasticsearch to be ready..."

# curl_es wraps curl to call Elasticsearch; it adds HTTP basic auth using ELASTIC_PASSWORD when set and runs curl with --fail.
curl_es() {
  if [ -n "$ELASTIC_PASSWORD" ]; then
    curl -f -s -u "elastic:$ELASTIC_PASSWORD" "$@"
  else
    curl -f -s "$@"
  fi
}

if [ -z "$ELASTIC_PASSWORD" ]; then
  echo "ERROR: ELASTIC_PASSWORD must be set when security is enabled on Elasticsearch."
  exit 1
fi

MAX_RETRIES="${ELASTIC_WAIT_RETRIES:-60}"
RETRY=0
until curl_es http://elasticsearch:9200 > /dev/null; do
  echo "Elasticsearch is still starting up..."
  sleep 5
  RETRY=$((RETRY + 1))
  if [ "$RETRY" -ge "$MAX_RETRIES" ]; then
    echo "ERROR: Elasticsearch not ready after $((MAX_RETRIES * 5))s"
    exit 1
  fi
done

echo "Elasticsearch is ready. Setting up ILM and Index Templates..."

# 1. Create ILM Policy (Delete after 7 days)
echo "Creating ILM Policy..."
RESPONSE=$(curl_es -w "\n%{http_code}" -X PUT "http://elasticsearch:9200/_ilm/policy/nexus-logs-policy" -H 'Content-Type: application/json' -d'
{
  "policy": {
    "phases": {
      "hot": {
        "actions": {
          "rollover": {
            "max_size": "50gb",
            "max_age": "1d"
          }
        }
      },
      "delete": {
        "min_age": "7d",
        "actions": {
          "delete": {}
        }
      }
    }
  }
}
')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
if [ "$HTTP_CODE" -ge 400 ]; then
  echo "ERROR: Failed to create ILM policy (HTTP $HTTP_CODE)"
  echo "$RESPONSE" | sed '$d'
  exit 1
fi
echo "ILM Policy created successfully."

# 2. Create Index Template
echo "Creating Index Template..."
RESPONSE=$(curl_es -w "\n%{http_code}" -X PUT "http://elasticsearch:9200/_index_template/nexus-logs-template" -H 'Content-Type: application/json' -d'
{
  "index_patterns": ["nexus-logs-*"],
  "template": {
    "settings": {
      "index.lifecycle.name": "nexus-logs-policy"
    }
  }
}
')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
if [ "$HTTP_CODE" -ge 400 ]; then
  echo "ERROR: Failed to create index template (HTTP $HTTP_CODE)"
  echo "$RESPONSE" | sed '$d'
  exit 1
fi
echo "Index Template created successfully."

# 3. Create Logstash Writer Role and User (Least Privilege)
echo "Setting up Logstash Writer role and user..."
# Create role
RESPONSE=$(curl_es -w "\n%{http_code}" -X PUT "http://elasticsearch:9200/_security/role/logstash_writer" -H 'Content-Type: application/json' -d'
{
  "cluster": ["monitor", "manage_ilm"],
  "indices": [
    {
      "names": ["nexus-logs-*"],
      "privileges": ["write", "create", "create_index", "manage", "read"]
    }
  ]
}
')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
if [ "$HTTP_CODE" -ge 400 ]; then
  echo "ERROR: Failed to create logstash_writer role (HTTP $HTTP_CODE)"
  echo "$RESPONSE" | sed '$d'
  exit 1
fi

# Create user
if [ -z "$LOGSTASH_PASSWORD" ]; then
  echo "ERROR: LOGSTASH_PASSWORD is not set"
  exit 1
fi
JSON_BODY=$(jq -n --arg pw "$LOGSTASH_PASSWORD" '{password: $pw, roles: ["logstash_writer"]}')
RESPONSE=$(curl_es -w "\n%{http_code}" -X PUT "http://elasticsearch:9200/_security/user/logstash_writer" -H 'Content-Type: application/json' -d "$JSON_BODY")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
if [ "$HTTP_CODE" -ge 400 ]; then
  echo "ERROR: Failed to create logstash_writer user (HTTP $HTTP_CODE)"
  echo "$RESPONSE" | sed '$d'
  exit 1
fi
echo "Logstash Writer role and user set up successfully."

# 4. Set password for kibana_system user (required for Kibana to connect)
if [ -n "$KIBANA_PASSWORD" ]; then
  echo "Setting password for kibana_system user..."
  JSON_BODY=$(jq -n --arg pw "$KIBANA_PASSWORD" '{password: $pw}')
  RESPONSE=$(curl_es -w "\n%{http_code}" -X POST "http://elasticsearch:9200/_security/user/kibana_system/_password" -H 'Content-Type: application/json' -d "$JSON_BODY")
  HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
  if [ "$HTTP_CODE" -ge 400 ]; then
    echo "ERROR: Failed to set kibana_system password (HTTP $HTTP_CODE)"
    echo "$RESPONSE" | sed '$d'
    exit 1
  fi
  echo "kibana_system password set successfully."
fi

echo "ELK Provisioning completed!"
