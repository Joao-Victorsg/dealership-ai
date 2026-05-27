import { useSyncExternalStore } from "react";
import { CARS as SEED_CARS, type Car, type CarStatus } from "./mock-data";

const KEY = "aurelio.cars.v1";
const SALES_KEY = "aurelio.sales.v1";

export interface SaleRecord {
  id: string;
  carId: string;
  manufacturer: string;
  model: string;
  year: number;
  saleValue: number; // includes tax
  netValue: number; // pre-tax
  date: string; // ISO
  buyer: string;
}

interface StoreState {
  cars: Car[];
  sales: SaleRecord[];
}

let state: StoreState = load();
const listeners = new Set<() => void>();

function load(): StoreState {
  if (typeof window === "undefined") return { cars: SEED_CARS, sales: seedSales() };
  try {
    const raw = localStorage.getItem(KEY);
    const cars = raw ? (JSON.parse(raw) as Car[]) : SEED_CARS;
    const rawSales = localStorage.getItem(SALES_KEY);
    const sales = rawSales ? (JSON.parse(rawSales) as SaleRecord[]) : seedSales();
    return { cars, sales };
  } catch {
    return { cars: SEED_CARS, sales: seedSales() };
  }
}

function seedSales(): SaleRecord[] {
  const now = new Date();
  const monthIdx = (offset: number) => {
    const d = new Date(now.getFullYear(), now.getMonth() - offset, 12);
    return d.toISOString().slice(0, 10);
  };
  return [
    {
      id: "s-2001",
      carId: "honda-civic-2023",
      manufacturer: "Honda",
      model: "Civic Touring",
      year: 2023,
      saleValue: 192780,
      netValue: 178500,
      date: monthIdx(0),
      buyer: "Ana Silva",
    },
    {
      id: "s-2002",
      carId: "fiat-pulse-2024",
      manufacturer: "Fiat",
      model: "Pulse Impetus",
      year: 2024,
      saleValue: 134892,
      netValue: 124900,
      date: monthIdx(0),
      buyer: "João Costa",
    },
    {
      id: "s-2003",
      carId: "byd-yuan-plus-2024",
      manufacturer: "BYD",
      model: "Yuan Plus",
      year: 2024,
      saleValue: 215892,
      netValue: 199900,
      date: monthIdx(0),
      buyer: "Carla Mendes",
    },
    {
      id: "s-2004",
      carId: "renault-zoe-2022",
      manufacturer: "Renault",
      model: "Zoe E-Tech",
      year: 2022,
      saleValue: 142560,
      netValue: 132000,
      date: monthIdx(1),
      buyer: "Pedro Lima",
    },
    {
      id: "s-2005",
      carId: "jeep-compass-2023",
      manufacturer: "Jeep",
      model: "Compass Limited",
      year: 2023,
      saleValue: 198720,
      netValue: 184000,
      date: monthIdx(1),
      buyer: "Marina Souza",
    },
    {
      id: "s-2006",
      carId: "toyota-hilux-2024",
      manufacturer: "Toyota",
      model: "Hilux SRX",
      year: 2024,
      saleValue: 336960,
      netValue: 312000,
      date: monthIdx(2),
      buyer: "Rafael Torres",
    },
    {
      id: "s-2007",
      carId: "audi-rs5-2023",
      manufacturer: "Audi",
      model: "RS5 Sportback",
      year: 2023,
      saleValue: 660960,
      netValue: 612000,
      date: monthIdx(3),
      buyer: "Helena Duarte",
    },
  ];
}

function persist() {
  if (typeof window === "undefined") return;
  localStorage.setItem(KEY, JSON.stringify(state.cars));
  localStorage.setItem(SALES_KEY, JSON.stringify(state.sales));
  listeners.forEach((l) => l());
}

export function getCars() {
  return state.cars;
}

export function getSales() {
  return state.sales;
}

export function findCarFromStore(id: string) {
  return state.cars.find((c) => c.id === id);
}

export function upsertCar(car: Car) {
  const idx = state.cars.findIndex((c) => c.id === car.id);
  const next = [...state.cars];
  if (idx >= 0) next[idx] = car;
  else next.unshift(car);
  state = { ...state, cars: next };
  persist();
}

export function deleteCar(id: string) {
  state = { ...state, cars: state.cars.filter((c) => c.id !== id) };
  persist();
}

export function setCarStatus(id: string, status: CarStatus) {
  state = {
    ...state,
    cars: state.cars.map((c) => (c.id === id ? { ...c, status } : c)),
  };
  persist();
}

export function recordSale(sale: SaleRecord) {
  state = { ...state, sales: [sale, ...state.sales] };
  persist();
}

export function resetStore() {
  state = { cars: SEED_CARS, sales: seedSales() };
  persist();
}

export function useCarStore() {
  return useSyncExternalStore(
    (cb) => {
      listeners.add(cb);
      return () => listeners.delete(cb);
    },
    () => state,
    () => ({ cars: SEED_CARS, sales: seedSales() }),
  );
}

export function slugify(input: string) {
  return input
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-|-$/g, "");
}
