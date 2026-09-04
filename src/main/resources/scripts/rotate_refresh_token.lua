-- KEYS[1] = family key
-- KEYS[2] = old token key
-- KEYS[3] = new token key
-- KEYS[4] = user sessions zset key
-- ARGV[1] = old token hash
-- ARGV[2] = new token hash
-- ARGV[3] = new token JSON
-- ARGV[4] = ttl in ms
-- ARGV[5] = now (epoch millis)

local currentHead = redis.call('GET', KEYS[1])

if not currentHead or currentHead ~= ARGV[1] then
    return currentHead or false
end

redis.call('DEL', KEYS[2])
redis.call('ZREM', KEYS[4], ARGV[1])

redis.call('SET', KEYS[3], ARGV[3], 'PX', ARGV[4])
redis.call('ZADD', KEYS[4], ARGV[5], ARGV[2])
redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[4])

return 'OK'