# EarthMAX GraphQL Operations

This directory contains all GraphQL operations for the EarthMAX application, including queries, mutations, and subscriptions.

## 📁 File Structure

### Queries
- `GetEcoTips.graphql` - Retrieve eco-tips with filtering and pagination
- `GetEcoTipById.graphql` - Get a specific eco-tip by ID
- `SearchEcoTips.graphql` - Advanced search with full-text search capabilities
- `GetUserProfile.graphql` - Retrieve comprehensive user profile information
- `GetUserAchievements.graphql` - Get user achievements with filtering

### Mutations
- `CreateEcoTip.graphql` - Create new eco-tips
- `UpdateEcoTip.graphql` - Update existing eco-tips
- `DeleteEcoTip.graphql` - Soft delete eco-tips
- `UpdateUserProfile.graphql` - Update user profile and preferences
- `CompleteEcoTip.graphql` - Mark eco-tips as completed and track progress

### Subscriptions
- `EcoTipUpdated.graphql` - Real-time eco-tip updates
- `UserAchievementUnlocked.graphql` - Achievement notifications
- `UserStatsUpdated.graphql` - Real-time user statistics updates

## 🔧 Schema Features

### Type Safety
- Comprehensive input types for all operations
- Proper error handling with structured error responses
- Consistent naming conventions following GraphQL best practices

### Performance Optimizations
- Cursor-based pagination for efficient data loading
- Connection patterns for scalable list queries
- Selective field querying to minimize data transfer

### Real-time Features
- WebSocket subscriptions for live updates
- Achievement notifications
- Statistics tracking and updates

## 📖 Usage Examples

### Basic Query
```graphql
query GetEcoTips($category: String, $limit: Int) {
  getEcoTips(category: $category, limit: $limit) {
    id
    title
    description
    category
    difficulty
  }
}
```

### Advanced Search
```graphql
query SearchEcoTips($searchTerm: String, $filter: EcoTipsFilterInput) {
  searchEcoTips(searchTerm: $searchTerm, filter: $filter) {
    edges {
      node {
        id
        title
        category
        difficulty
      }
    }
    pageInfo {
      hasNextPage
      endCursor
    }
  }
}
```

### Mutation with Error Handling
```graphql
mutation CreateEcoTip($input: CreateEcoTipInput!) {
  createEcoTip(input: $input) {
    success
    message
    errors {
      field
      code
      message
    }
    ecoTip {
      id
      title
    }
  }
}
```

## 🚀 Best Practices

1. **Always handle errors** - Check the `errors` field in mutation responses
2. **Use pagination** - Implement proper pagination for list queries
3. **Selective querying** - Only request fields you need
4. **Input validation** - Validate inputs on the client side before sending
5. **Caching** - Leverage Apollo Client caching for better performance

## 🔗 Related Files

- `schema.graphqls` - Complete GraphQL schema definition
- Android data layer implementation in `data/` module
- Repository patterns in `repository/` package