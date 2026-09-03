local stockKey = KEYS[1]
local usersKey = KEYS[2]
local statusKey = KEYS[3]
local userId = ARGV[1]
local pendingTtlSeconds = tonumber(ARGV[2])

if redis.call('SISMEMBER', usersKey, userId) == 1 then
    return -1
end

local stockValue = redis.call('GET', stockKey)
if stockValue == false then
    stockValue = '0'
end

local stock = tonumber(stockValue)
if stock <= 0 then
    return 0
end

redis.call('DECR', stockKey)
redis.call('SADD', usersKey, userId)
redis.call('SET', statusKey, 'PENDING', 'EX', pendingTtlSeconds)

return 1
