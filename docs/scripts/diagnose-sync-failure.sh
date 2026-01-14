#!/bin/bash

# Diagnostic script for RepoSync workflow failures
# This script helps identify the root cause of sync failures

echo "============================================"
echo "  RepoSync Workflow Failure Diagnostics"
echo "============================================"
echo ""

# Check if sync-result.json exists
if [ ! -f "sync-result.json" ]; then
    echo "❌ ERROR: sync-result.json not found"
    echo "The sync may not have been triggered or the API call failed"
    exit 1
fi

# Display the raw JSON
echo "=== Sync Result JSON ==="
cat sync-result.json
echo "========================"
echo ""

# Parse the status and error
STATUS=$(jq -r '.status // "UNKNOWN"' sync-result.json)
ERROR_MSG=$(jq -r '.errorMessage // "No error message"' sync-result.json)

echo "Status: $STATUS"
echo "Error Message: $ERROR_MSG"
echo ""

# Common failure patterns and solutions
echo "=== Common Failure Patterns ==="
echo ""

if echo "$ERROR_MSG" | grep -qi "connection refused"; then
    echo "🔍 Pattern: Connection Refused"
    echo "   Likely Cause: A downstream service (github-service, milvus-service, etc.) is not running or crashed"
    echo "   Solution: Check the service logs below to see which service failed"
    echo ""
elif echo "$ERROR_MSG" | grep -qi "401\|unauthorized\|bad credentials"; then
    echo "🔍 Pattern: Authentication Error (401)"
    echo "   Likely Cause: Invalid or expired GitHub token"
    echo "   Solution: Verify REPOSYNC_GITHUB_TOKEN secret is valid and has correct permissions"
    echo "   Required permissions: repo, read:org"
    echo ""
elif echo "$ERROR_MSG" | grep -qi "403\|forbidden"; then
    echo "🔍 Pattern: Authorization Error (403)"
    echo "   Likely Cause: GitHub token lacks permissions for the organization"
    echo "   Solution: Ensure the token user is a member of REPOSYNC_ORGANIZATION"
    echo ""
elif echo "$ERROR_MSG" | grep -qi "timeout\|timed out"; then
    echo "🔍 Pattern: Timeout"
    echo "   Likely Cause: Service is too slow to respond (Azure OpenAI, Milvus, etc.)"
    echo "   Solution: Check network connectivity and service health"
    echo ""
elif echo "$ERROR_MSG" | grep -qi "No repositories found"; then
    echo "🔍 Pattern: No Repositories Found"
    echo "   Likely Cause: REPOSYNC_FILTER_KEYWORD is too restrictive"
    echo "   Solution: Verify the filter keyword matches actual repository names"
    echo ""
elif echo "$ERROR_MSG" | grep -qi "Step 1"; then
    echo "🔍 Pattern: Step 1 Failure (Fetch Repositories)"
    echo "   Likely Cause: GitHub service or authentication issue"
    echo "   Check: github-service logs and REPOSYNC_GITHUB_TOKEN"
    echo ""
elif echo "$ERROR_MSG" | grep -qi "Step 3"; then
    echo "🔍 Pattern: Step 3 Failure (Document Chunking)"
    echo "   Likely Cause: Document processor service issue"
    echo "   Check: document-processor-service logs"
    echo ""
elif echo "$ERROR_MSG" | grep -qi "Step 4"; then
    echo "🔍 Pattern: Step 4 Failure (Generate Embeddings)"
    echo "   Likely Cause: Azure OpenAI API issue or invalid credentials"
    echo "   Check: AZURE_OPENAI_API_KEY, AZURE_OPENAI_ENDPOINT, AZURE_OPENAI_EMBEDDINGS_DEPLOYMENT"
    echo ""
elif echo "$ERROR_MSG" | grep -qi "Step 5\|Step 6"; then
    echo "🔍 Pattern: Step 5/6 Failure (Milvus Operations)"
    echo "   Likely Cause: Milvus connection or authentication issue"
    echo "   Check: MILVUS_URI, MILVUS_TOKEN, MILVUS_COLLECTION_NAME"
    echo ""
fi

# Service logs analysis
echo "=== Service Logs Analysis ==="
echo ""

for service in orchestrator-service github-service document-processor-service embedding-service milvus-service; do
    LOG_FILE="${service}/${service}.log"
    if [ -f "$LOG_FILE" ]; then
        echo "--- $service ---"

        # Check for common error patterns
        if grep -qi "error\|exception\|failed" "$LOG_FILE"; then
            echo "⚠️  Errors detected:"
            grep -i "error\|exception\|failed" "$LOG_FILE" | tail -n 5
        else
            echo "✓ No obvious errors"
        fi

        # Show last few lines
        echo "Last 3 lines:"
        tail -n 3 "$LOG_FILE"
        echo ""
    else
        echo "--- $service ---"
        echo "⚠️  Log file not found: $LOG_FILE"
        echo ""
    fi
done

echo "============================================"
echo "  Diagnostics Complete"
echo "============================================"
echo ""
echo "💡 Next Steps:"
echo "1. Review the error pattern identified above"
echo "2. Check the relevant service logs"
echo "3. Verify all required secrets/environment variables are set correctly"
echo "4. For full logs, run: tail -n 150 orchestrator-service/orchestrator-service.log"

