#!/bin/sh

# Wait for Elasticsearch to be ready
echo "Waiting for Elasticsearch to be ready..."
# Use basic auth if ELASTIC_PASSWORD is set
AUTH=""
if [ ! -z "$ELASTIC_PASSWORD" ]; then
  AUTH="-u elastic:$ELASTIC_PASSWORD"
fi

until curl -s $AUTH http://elasticsearch:9200 > /dev/null; do
  echo "Elasticsearch is still starting up..."
  sleep 5
done

echo "Elasticsearch is ready. Setting up ILM and Index Templates..."

# 1. Create ILM Policy (Delete after 7 days)
curl $AUTH -X PUT "http://elasticsearch:9200/_ilm/policy/nexus-logs-policy" -H 'Content-Type: application/json' -d'
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
'

# 2. Create Index Template
curl $AUTH -X PUT "http://elasticsearch:9200/_index_template/nexus-logs-template" -H 'Content-Type: application/json' -d'
{
  "index_patterns": ["nexus-logs-*"],
  "template": {
    "settings": {
      "index.lifecycle.name": "nexus-logs-policy"
    }
  }
}
'

echo "ELK Provisioning completed!"
