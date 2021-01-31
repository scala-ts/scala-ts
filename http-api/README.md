# Demo REST API

REST API implemented using Akka HTTP.

## Routes

The API routes are described below.

> See [Postman collection](./src/test/postman/collection.json) (make sure to set the `backendUrl` variable)

### Signup

**Request path:** `/user/signup`

**Request content type:** `application/json`

**Request payload:** [`Account`](../common/src/main/scala/Account.scala) to be created

**Response content type:** `application/json`

**Response payload:**

If successful with status code `201 Created`, the name of the created user (as JSON string).

Otherwise the error type and details is sent back as JSON. e.g.

```
> 500 Internal Server Error

{
    "error": "validation",
    "details": {
        "obj.userName": [
            {
                "msg": [
                    "error.path.missing"
                ],
                "args": []
            }
        ]
    }
}
```


