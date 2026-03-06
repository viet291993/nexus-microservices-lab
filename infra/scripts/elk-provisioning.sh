#!/bin/sh

# Wait for Elasticsearch to be ready
echo "Waiting for Elasticsearch to be ready..."
# Use basic auth if ELASTIC_PASSWORD is set
AUTH_ARGS=()
if [ ! -z "$ELASTIC_PASSWORD" ]; then
  AUTH_ARGS=(-u "elastic:$ELASTIC_PASSWORD")
fi

until curl -s "${AUTH_ARGS[@]}" http://elasticsearch:9200 > /dev/null; do
  echo "Elasticsearch is still starting up..."
  sleep 5
done

echo "Elasticsearch is ready. Setting up ILM and Index Templates..."

# 1. Create ILM Policy (Delete after 7 days)
echo "Creating ILM Policy..."
RESPONSE=$(curl -s -w "\n%{http_code}" "${AUTH_ARGS[@]}" -X PUT "http://elasticsearch:9200/_ilm/policy/nexus-logs-policy" -H 'Content-Type: application/json' -d'
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
RESPONSE=$(curl -s -w "\n%{http_code}" "${AUTH_ARGS[@]}" -X PUT "http://elasticsearch:9200/_index_template/nexus-logs-template" -H 'Content-Type: application/json' -d'
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

# 3. Set password for kibana_system user (required for Kibana to connect)
if [ ! -z "$KIBANA_PASSWORD" ]; then
  echo "Setting password for kibana_system user..."
  RESPONSE=$(curl -s -w "\n%{http_code}" "${AUTH_ARGS[@]}" -X POST "http://elasticsearch:9200/_security/user/kibana_system/_password" -H 'Content-Type: application/json' -d"
  {
    \"password\": \"$KIBANA_PASSWORD\"
  }
  ")
  HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
  if [ "$HTTP_CODE" -ge 400 ]; then
    echo "ERROR: Failed to set kibana_system password (HTTP $HTTP_CODE)"
    echo "$RESPONSE" | sed '$d'
    exit 1
  fi
  echo "kibana_system password set successfully."
fi

echo "ELK Provisioning completed!"
