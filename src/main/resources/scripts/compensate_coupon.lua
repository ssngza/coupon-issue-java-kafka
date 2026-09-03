local stockKey = KEYS[1]
local usersKey = KEYS[2]
local statusKey = KEYS[3]
local userId = ARGV[1]
local failedTtlSeconds = tonumber(ARGV[2])

-- DLT 또는 Producer 실패 보상도 세 Redis 변경을 하나의 원자 연산으로 묶습니다.
if redis.call('SREM', usersKey, userId) == 1 then
    redis.call('INCR', stockKey)
end
redis.call('SET', statusKey, 'FAILED', 'EX', failedTtlSeconds)
return 1
