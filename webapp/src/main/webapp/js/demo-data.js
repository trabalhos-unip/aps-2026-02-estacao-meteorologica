// Série local e determinística para apresentar as telas sem alterar o coletor ou o MySQL.
const anchor = new Date();

function timestamp(date) {
  const pad = value => String(value).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

function weatherAt(dayIndex, hour) {
  const cycle = Math.sin((hour - 7) * Math.PI / 12);
  const front = Math.sin(dayIndex * .63);
  const rainy = dayIndex % 6 === 2 || dayIndex % 9 === 5;
  const rain = rainy && hour >= 11 && hour <= 17
    ? Math.max(0, 1.1 + 1.4 * Math.sin((hour - 11) * Math.PI / 6) + .35 * front)
    : 0;
  return {
    temperatura: Math.round((22 + 5.7 * cycle + 2.2 * front - (rain ? 1.7 : 0)) * 10) / 10,
    umidadeRelativa: Math.round(Math.max(35, Math.min(98, 67 - 17 * cycle + 7 * front + (rain ? 16 : 0)))),
    precipitacao: Math.round(rain * 10) / 10,
    velocidadeVento: Math.round((8 + 3.4 * Math.abs(Math.sin(hour * .45 + dayIndex)) + 2.1 * Math.abs(front)) * 10) / 10,
  };
}

const readings = [];
for (let daysAgo = 29; daysAgo >= 0; daysAgo--) {
  const day = new Date(anchor.getFullYear(), anchor.getMonth(), anchor.getDate() - daysAgo);
  const dayIndex = Math.round(day.getTime() / 86_400_000);
  const lastHour = daysAgo === 0 ? anchor.getHours() : 23;
  for (let hour = 0; hour <= lastHour; hour++) {
    const at = new Date(day.getFullYear(), day.getMonth(), day.getDate(), hour);
    const weather = weatherAt(dayIndex, hour);
    readings.push({
      id: -readings.length - 1,
      dataHoraColeta: timestamp(at),
      ...weather,
      presencaPrecipitacao: weather.precipitacao > 0,
      descricaoClima: weather.precipitacao > 0 ? "Chuva passageira" : hour >= 7 && hour <= 17 ? "Parcialmente ensolarado" : "Céu limpo",
      fonteDados: "Simulado",
    });
  }
}

// A leitura atual acompanha o relógio, além do ponto agregado da hora.
const current = weatherAt(Math.round(new Date(anchor.getFullYear(), anchor.getMonth(), anchor.getDate()).getTime() / 86_400_000), anchor.getHours());
readings.push({
  id: -readings.length - 1,
  dataHoraColeta: timestamp(anchor),
  ...current,
  presencaPrecipitacao: current.precipitacao > 0,
  descricaoClima: current.precipitacao > 0 ? "Chuva passageira" : "Parcialmente ensolarado",
  fonteDados: "Simulado",
});

export const demoApi = {
  buscarUltima: async () => readings.at(-1),
  buscarUltimas: async (limit = 100) => readings.slice(-limit),
  buscarPorPeriodo: async (start, end) => {
    const first = new Date(start).getTime();
    const last = new Date(end).getTime();
    return readings.filter(reading => {
      const at = new Date(reading.dataHoraColeta).getTime();
      return at >= first && at <= last;
    });
  },
  buscarTotal: async () => ({ total: readings.length }),
};

export function demoForecast() {
  return Array.from({ length: 5 }, (_, index) => {
    const day = new Date(anchor.getFullYear(), anchor.getMonth(), anchor.getDate() + index);
    const dayIndex = Math.round(day.getTime() / 86_400_000);
    const values = Array.from({ length: 24 }, (_, hour) => weatherAt(dayIndex, hour));
    return {
      day,
      minimum: Math.min(...values.map(value => value.temperatura)),
      maximum: Math.max(...values.map(value => value.temperatura)),
      rain: Math.round(values.reduce((sum, value) => sum + value.precipitacao, 0) * 10) / 10,
    };
  });
}
